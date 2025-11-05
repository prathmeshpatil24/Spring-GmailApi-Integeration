package com.gmailIntegeration.Service;

import com.gmailIntegeration.Configuration.GmailConfig1;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Session;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.security.GeneralSecurityException;

import java.util.*;

@Service
public class GmailService {

    private final GmailConfig1 gmailConfig;

    public GmailService(GmailConfig1 gmailConfig) {
        this.gmailConfig = gmailConfig;
    }

    // helper method to create Gmail instance
    private Gmail getGmail(String userEmail) throws Exception {
        try{
            Credential credential = gmailConfig.getStoredCredential(userEmail);
            if (credential == null) {
                String authorizationUrl= gmailConfig.getAuthorizationUrl();
                throw new IllegalStateException("No credential found for user: " + userEmail +
                        ". Please authenticate using the following URL: " +
                        authorizationUrl);
            }
            return new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("Gmail API Spring Boot").build();

        }catch (IOException e) {
            throw new IOException("Failed to load Gmail credentials: " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new GeneralSecurityException("Security error initializing Gmail service: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new Exception("Unexpected error creating Gmail instance: " + e.getMessage(), e);
        }
    }

    public List<String> currentUserProfile(String userEmail) throws Exception {
        Gmail gmail = getGmail(userEmail);

        try {
            Profile profile = gmail.users().getProfile("me").execute();

            ArrayList<String> profileList = new ArrayList<>();
            System.out.println("User Email: " + profile.getEmailAddress());
            System.out.println("Messages Total: " + profile.getMessagesTotal());
            System.out.println("Threads Total: " + profile.getThreadsTotal());
//            Map<String, Object> profile = new HashMap<>();// for dynamic key-value pairs
            for (Map.Entry<String, Object> entry : profile.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                profileList.add(key + ": " + value);
            }
            return profileList;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user profile: " + e.getMessage(), e);
        }
    }

    //listing all labels in Gmail account
    public List<String> listLabels(String userEmail) throws Exception {
        try {
            Gmail gmail = getGmail(userEmail);

            ListLabelsResponse response = gmail.users().labels().list("me").execute();
            List<String> labelNames = new ArrayList<>();

            for (Label label : response.getLabels()) {
                labelNames.add(label.getName());
                System.out.println("Label Name: " + label.getName() + " | ID: " + label.getId());
                System.out.println("---------------");
            }
            return labelNames;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //listing emails by label dynamically
// listing emails by label dynamically (optimized)
    public List<Map<String, Object>> getEmailsByLabel(String userEmail, String label) throws Exception {
        Gmail gmail = getGmail(userEmail);
        List<Map<String, Object>> emailsList = Collections.synchronizedList(new ArrayList<>());

        try {
            // Step 1: Get message IDs for the label
            ListMessagesResponse response = gmail.users().messages()
                    .list("me")
                    .setLabelIds(Collections.singletonList(label))
                    .setMaxResults(10L)
                    .execute();

            List<Message> messages = response.getMessages();

            if (messages == null || messages.isEmpty()) {
                System.out.println("No emails found with label: " + label);
                return Collections.emptyList();
            }

            // Step 2: Create a BatchRequest to fetch all message metadata in parallel
            BatchRequest batch = gmail.batch();
            //Creates an empty batch container tied to the Gmail client.

            for (Message msg : messages) {
                gmail.users()
                        .messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        //Instead of calling .execute() on that request, you call .queue(...) which adds the request to the batch.
                        //You pass a JsonBatchCallback<Message> which defines onSuccess and onFailure callbacks for that request.
                        //The callback will be invoked later, after batch.execute() is called and the batch response is processed.
                        .queue(batch, new JsonBatchCallback<Message>() {
                            @Override
                            public void onSuccess(Message message, HttpHeaders responseHeaders) {
                                String subject = "", from = "", dateTime = "";

                                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                                    switch (header.getName()) {
                                        case "Subject" -> subject = header.getValue();
                                        case "From" -> from = header.getValue();
                                        case "Date" -> dateTime = header.getValue();
                                    }
                                }

                                Map<String, Object> emailInfo = new HashMap<>();
                                emailInfo.put("ID", message.getId());
                                emailInfo.put("From", from);
                                emailInfo.put("Subject", subject);
                                emailInfo.put("Date", dateTime);
                                emailsList.add(emailInfo);
                            }

                            @Override
                            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                                System.err.println("❌ Failed to fetch message: " + e.getMessage());
                            }
                        });
            }

            // Step 3: Execute all requests in one batch call
            batch.execute();

            // Step 4: Print summary
            emailsList.forEach(email -> {
                System.out.println(email);
                System.out.println("-----------------------------------");
            });

            return emailsList;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch emails by label: " + e.getMessage(), e);
        }
    }


    //listing inbox emails
    public List<String> getInboxEmails(String userEmail) throws Exception {

        try {

            Gmail gmail = getGmail(userEmail);
            List<String> emailList = new ArrayList<>();

            ListMessagesResponse response = gmail.users()
                    .messages()
                    .list("me")
                    .setLabelIds(Arrays.asList(
                            "INBOX",
                            "CATEGORY_PERSONAL"
                    ))
                    .setIncludeSpamTrash(false)
                    .setPrettyPrint(true)
                    .setMaxResults(50L)
                    .execute();

            //System.out.println(response.getNextPageToken());// for pagination

            List<Message> messages = response.getMessages();

            if (messages == null || messages.isEmpty()) {
                emailList.add("No emails found in inbox.");
                return emailList;
            }

            for (Message msg : messages) {
                String messageId = msg.getId();  //this is your unique ID
                Message message = gmail.users()
                        .messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .execute();

                String subject = "", from = "", dateTime = "";

                //Iterates through all message headers and extracts specific ones
                for (MessagePartHeader header : message.getPayload().getHeaders()) {

                    //header.getName() gives the key like Subject, From, To, Date,  etc.
                    if ("Subject".equalsIgnoreCase(header.getName())){
                        subject = header.getValue();// gets the value of the header
                    } else if ("From".equalsIgnoreCase(header.getName())){
                        from = header.getValue();
                    } else if ("Date".equalsIgnoreCase(header.getName())) {
                        dateTime = header.getValue();
                    }
                }
                emailList.add("ID: " + messageId + "| 📩 From: " + from + " | Subject: " + subject + "| Date: " + dateTime );// for testing purpose
            }
            for (String emailInfo : emailList) {
                System.out.println(emailInfo);
            }
            return emailList;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    //starring and un-starring email
    public void toggleStar(String userEmail, String messageId, boolean isStarred) throws Exception {
        Gmail gmail = getGmail(userEmail);

        try{

            ModifyMessageRequest mod = new ModifyMessageRequest();

            if (isStarred) {
                mod.setAddLabelIds(Collections.singletonList("STARRED"));
                System.out.println("Un-starring email..." + messageId);
            } else {
                mod.setRemoveLabelIds(Collections.singletonList("STARRED"));
                System.out.println("Starring email..." + messageId);
            }

            gmail.users().messages().modify("me", messageId, mod).execute();
            System.out.println("Toggle complete for message: " + messageId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to toggle star for message: " + messageId, e);
        }
    }

    //listing starred emails
    public List<String> getStarredEmails(String userEmail) throws Exception{
        Gmail gmail = getGmail(userEmail);

        try {
            ArrayList<String>staredEmailList = new ArrayList<>();

            ListMessagesResponse response = gmail.users().messages()
                    .list("me")
                    .setLabelIds(Collections.singletonList("STARRED"))
                    .execute();

            List<Message> starredMessages = response.getMessages();

            if (starredMessages == null || starredMessages.isEmpty()) {
                System.out.println("No starred emails found.");
                staredEmailList.add("No starred emails found.");
                return Collections.emptyList();
            }

//            for (Message message : starredMessages) {
//                System.out.println("Starred Email ID: " + message.getId());
//            }

            for (Message msg : starredMessages) {
                String messageId = msg.getId();  //this is your unique ID
                Message message = gmail.users()
                        .messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .execute();

                String subject = "", from = "", dateTime = "";

                //Iterates through all message headers and extracts specific ones
                for (MessagePartHeader header : message.getPayload().getHeaders()) {

                    //header.getName() gives the key like Subject, From, To, Date,  etc.
                    if ("Subject".equalsIgnoreCase(header.getName())){
                        subject = header.getValue();// gets the value of the header
                    } else if ("From".equalsIgnoreCase(header.getName())){
                        from = header.getValue();
                    } else if ("Date".equalsIgnoreCase(header.getName())) {
                        dateTime = header.getValue();
                    }
                }
                staredEmailList.add("ID: " + messageId + "| 📩 From: " + from + " | Subject: " + subject + "| Date: " + dateTime );// for testing purpose
            }

            for (String staredEmailInfo : staredEmailList) {
                System.out.println(staredEmailInfo);
                System.out.println("-----------------------------------");
            }

            System.out.println(staredEmailList.size());

            return staredEmailList;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //sending email
    public void sendEmail(String userEmail,
                          String toEmail,
//                          String Bcc,
//                          String Ccc,
                          String subject,
                          String bodyText) throws Exception {

       try {
           Gmail gmail = getGmail(userEmail);
//          Properties properties = new Properties();
           //multiple recipients (To, Cc, Bcc) this also we can do

           String rawEmail = "From: " + userEmail + "\r\n" +
                   "To: " + toEmail + "\r\n" +
                   // here we can add Cc and Bcc if needed
//                   "Bcc: " + "\r\n" +
//                   "Cc: " + "\r\n" +
                   "Subject: " + subject + "\r\n" +
                   "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
                   bodyText;

           Message message = new Message();

           //Gmail API requires the message content to be Base64 URL-safe encoded (as per RFC 4648).
           //This encoding ensures that the email content can be safely transmitted over protocols that may not handle binary data correctly.
           //This ensures that special characters (+, /, etc.) don’t break when transmitted over HTTP.
           message.setRaw(Base64.getUrlEncoder()
                   .encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8)));

           gmail.users().messages().send("me", message).execute();//actual API call that sends our email through Gmail’s servers.
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
    }

    public void sendEmailWithAttachment(String userEmail,
                                        String toEmail,
//                                        String CC,
//                                        String BCC,
                                        String subject,
                                        String bodyText,
                                        MultipartFile attachmentFile) throws Exception {

        Gmail gmail = getGmail(userEmail);

       try {
           Properties props = new Properties();
           Session session = Session.getDefaultInstance(props, null);

           MimeMessage email = new MimeMessage(session);
           email.setFrom(new InternetAddress(userEmail));
           email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(toEmail));

//           // Optional: add CC recipients
//           if (ccEmail != null && !ccEmail.isEmpty()) {
//               email.addRecipient(jakarta.mail.Message.RecipientType.CC, new InternetAddress(ccEmail));
//           }
//
//           // Optional: add BCC recipients
//           if (bccEmail != null && !bccEmail.isEmpty()) {
//               email.addRecipient(jakarta.mail.Message.RecipientType.BCC, new InternetAddress(bccEmail));
//           }

           email.setSubject(subject, "UTF-8");

           MimeBodyPart textPart = new MimeBodyPart();
           textPart.setText(bodyText, "UTF-8");

           MimeMultipart multipart = new MimeMultipart();
           multipart.addBodyPart(textPart);

           //Handle optional MultipartFile safely
           if (attachmentFile != null && !attachmentFile.isEmpty()) {
               try {
                   MimeBodyPart attachmentPart = new MimeBodyPart();

                   // You can directly use MultipartFile’s input stream and content type
                   DataSource source = new ByteArrayDataSource(
                           attachmentFile.getInputStream(),
                           attachmentFile.getContentType() != null
                                   ? attachmentFile.getContentType()
                                   : "application/octet-stream"
                   );
                   attachmentPart.setDataHandler(new DataHandler(source));
                   attachmentPart.setFileName(
                           MimeUtility.encodeText(attachmentFile.getOriginalFilename())
                   );
                   multipart.addBodyPart(attachmentPart);
               } catch (Exception e) {
                   System.err.println("Failed to attach file: " + e.getMessage());
               }
           } else {
               System.out.println("No attachment provided. Sending email without attachment.");
           }

           email.setContent(multipart);

           ByteArrayOutputStream buffer = new ByteArrayOutputStream();
           email.writeTo(buffer);

           String encodedEmail = Base64.getUrlEncoder()
                   .encodeToString(buffer.toByteArray());

           Message message = new Message();
           message.setRaw(encodedEmail);

           gmail.users().messages().send("me", message).execute();

           System.out.println("Email sent successfully" +
                   (attachmentFile != null && !attachmentFile.isEmpty()
                           ? " with attachment: " + attachmentFile.getOriginalFilename()
                           : " without attachment."));

       } catch (Exception e) {
           throw new RuntimeException(e);
       }
    }

    //listing sent emails
    public List<String>sentEmailList(String userEmail) throws Exception{
        Gmail gmail = getGmail(userEmail);

        try {

            ListMessagesResponse listOfSentEmails = gmail.users().messages().list("me").setLabelIds(Arrays.asList("SENT")).execute();
            List<String> sentEmailList = new ArrayList<>();

            if (listOfSentEmails == null || listOfSentEmails.isEmpty()){
                sentEmailList.add("No emails found in inbox.");
                return sentEmailList;
            }

            for (Message message : listOfSentEmails.getMessages()) {
                String messageId = message.getId();
                Message fullMessage = gmail.users().messages().get("me", messageId).setFormat("metadata").execute();

                String subject = "", to = "", dateTime = "";

                for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
                    if ("Subject".equalsIgnoreCase(header.getName())) {
                        subject = header.getValue();
                    } else if ("To".equalsIgnoreCase(header.getName())) {
                        to = header.getValue();
                    } else if ("Date".equalsIgnoreCase(header.getName())) {
                        dateTime = header.getValue();
                    }
                }
                sentEmailList.add("ID: " + messageId + "| 📩 To: " + to + " | Subject: " + subject + "| Date: " + dateTime);
            }
            for (String sentEmailInfo : sentEmailList) {
                System.out.println(sentEmailInfo);
            }
            return sentEmailList;
        }catch (Exception ex){
            throw new RuntimeException(ex.getMessage());
        }
    }

    //listing draft emails
    public List<String> draftEmailsList(String userEmails) throws Exception{
        Gmail gmail = getGmail(userEmails);
        try {
            ListDraftsResponse listDraftsResponse = gmail.users().drafts().list("me").execute();

            List<String> draftEmailList = new ArrayList<>();

            if (listDraftsResponse == null || listDraftsResponse.isEmpty()){
                draftEmailList.add("No draft emails found.");
                return draftEmailList;
            }

            for (Draft draft : listDraftsResponse.getDrafts()) {
                String draftId = draft.getId();
                Message fullMessage = gmail.users().drafts().get("me", draftId).execute().getMessage();

                String subject = "", to = "", dateTime = "";

                for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
                    if ("Subject".equalsIgnoreCase(header.getName())) {
                        subject = header.getValue();
                    } else if ("To".equalsIgnoreCase(header.getName())) {
                        to = header.getValue();
                    } else if ("Date".equalsIgnoreCase(header.getName())) {
                        dateTime = header.getValue();
                    }
                }
                draftEmailList.add("ID: " + draftId + "| 📩 To: " + to + " | Subject: " + subject + "| Date: " + dateTime);
            }
            for (String draftEmailInfo : draftEmailList) {
                System.out.println(draftEmailInfo);
                System.out.println("-----------------------------------");
            }

            System.out.println("Total Draft Emails: " + draftEmailList.size());
            System.out.println("-----------------------------------");

            return draftEmailList;

        }catch (Exception ex){
            throw new RuntimeException(ex.getMessage());
        }
    }

    //listing spam emails
    public List<String>listOfSpamEmail(String userEmail) throws Exception{
        Gmail gmail = getGmail(userEmail);

        try {
            ListMessagesResponse listMessagesResponse = gmail.users()
                    .messages()
                    .list("me")
                    .setLabelIds(Collections.singletonList("SPAM"))
                    .setIncludeSpamTrash(false)
                    .setPrettyPrint(true)
                    .setMaxResults(50L)
                    .execute();

            ArrayList<String>listOfSpam = new ArrayList<>();

            List<Message> listOfSpamMessages = listMessagesResponse.getMessages();

            if (listOfSpamMessages == null || listOfSpamMessages.isEmpty()) {
                listOfSpam.add("No emails found in inbox.");
                return listOfSpam;
            }

            for (Message message:listOfSpamMessages){
                String messageId = message.getId();  //this is your unique ID
                Message message1 = gmail.users()
                        .messages()
                        .get("me", message.getId())
                        .setFormat("metadata")
                        .execute();

                String subject = "", from = "", dateTime = "";

                //Iterates through all message headers and extracts specific ones
                for (MessagePartHeader header : message.getPayload().getHeaders()) {

                    //header.getName() gives the key like Subject, From, To, Date,  etc.
                    if ("Subject".equalsIgnoreCase(header.getName())){
                        subject = header.getValue();// gets the value of the header
                    } else if ("From".equalsIgnoreCase(header.getName())){
                        from = header.getValue();
                    } else if ("Date".equalsIgnoreCase(header.getName())) {
                        dateTime = header.getValue();
                    }
                }
                listOfSpam.add("ID: " + messageId + "| 📩 From: " + from + " | Subject: " + subject + "| Date: " + dateTime );

            }

            for (String spamMessage:listOfSpam){
                System.out.println(spamMessage);
                System.out.println("----------------------------------");
            }

            System.out.println("Total Spam Emails: " + listOfSpam.size());
            System.out.println("-----------------------------------");

            return listOfSpam;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

     //fetching and displaying draft email body
    public Map<String,Object> readAnyEmailBody(String userEmail, String messageId, boolean isDraft) throws Exception {
        Gmail gmail = getGmail(userEmail);
        try {

            Message message;
            if (isDraft){
                // Get full draft message
                Draft draft = gmail.users()
                        .drafts()
                        .get("me", messageId)
                        .execute();

                message = draft.getMessage();

            }else {
                message = gmail.users()
                        .messages()
                        .get("me", messageId)
                        .setFormat("full")
                        .execute();
            }

            // Extract headers
            String subject = "", to = "", dateTime = "";
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                if ("Subject".equalsIgnoreCase(header.getName())) {
                    subject = header.getValue();
                } else if ("To".equalsIgnoreCase(header.getName())) {
                    to = header.getValue();
                } else if ("Date".equalsIgnoreCase(header.getName())) {
                    dateTime = header.getValue();
                }
            }

            // Extract body
            String body = extractBodyFromMessage(message);

            Map<String,Object>emailDetails = new HashMap<>();
            System.out.println("📧 To: " + to);
            System.out.println("📝 Subject: " + subject);
            System.out.println("📅 Date: " + dateTime);
            System.out.println("📨 Body:\n" + body);

            emailDetails.put("to",to);
            emailDetails.put("subject",subject);
            emailDetails.put("dateTime",dateTime);
            emailDetails.put("body",body);


            return emailDetails;

        } catch (Exception e) {
            throw new RuntimeException("Failed to read email: " + e.getMessage());
        }
    }

    // move to trash
    public void moveEmailToTrash(String userEmail, String messageId) throws Exception {
        Gmail gmail = getGmail(userEmail);
        try {
            gmail.users().messages().trash("me", messageId).execute();
            System.out.println("Email moved to Trash successfully!");
        } catch (Exception e) {
            throw new RuntimeException("Failed to move email to Trash: " + e.getMessage());
        }
    }

    //untrash email
    public void unTrashEmail(String userEmail, String messageId) throws Exception {
        Gmail gmail = getGmail(userEmail);
        try {
            gmail.users().messages().untrash("me", messageId).execute();
            System.out.println("Email moved to Trash successfully!");
        } catch (Exception e) {
            throw new RuntimeException("Failed to move email to Trash: " + e.getMessage());
        }
    }

    //deleting email permanently
    public void deleteEmail(String userEmail, String messageId) throws Exception {

        Gmail gmail = getGmail(userEmail);

        try {
            //messages():- Access the messages sub-resource (emails in that account)
            //delete("me", messageId):- Deletes the specified email message from the user’s mailbox.
            //Note:
            //If the email is in Trash or Spam, Gmail may delete it permanently.
            //If it’s in the Inbox, Gmail moves it to Trash by default.
            gmail.users().messages().delete("me", messageId).execute();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //helper method to decode Base64 URL-safe encoded strings
    private String decodeBase64(String encoded) {
        if (encoded == null) return "";
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    //helper method to extract body content from Message object
    private String extractBodyFromMessage(Message message) throws IOException {

        if (message.getPayload() == null) return "";

        //Retrieves the main structure of the email’s body and attachments
        //A MessagePart represents one "piece" of an email — e.g., plain text, HTML, or an attachment
        MessagePart payload = message.getPayload();

        if (payload.getParts() == null || payload.getParts().isEmpty()) {
            // Single-part message (usually plain text)
            return decodeBase64(payload.getBody().getData());//converts it into a readable string
        }

        // Multi-part message (HTML, attachments, etc.)
        for (MessagePart part : payload.getParts()) {
            String mimeType = part.getMimeType();
            if ("text/html".equalsIgnoreCase(mimeType)) {
                return decodeBase64(part.getBody().getData());
            } else if ("text/plain".equalsIgnoreCase(mimeType)) {
                // fallback
                return decodeBase64(part.getBody().getData());
            }

            // Handle nested parts (like multipart/alternative)
            if (part.getParts() != null && !part.getParts().isEmpty()) {
                for (MessagePart inner : part.getParts()) {
                    if ("text/html".equalsIgnoreCase(inner.getMimeType())) {
                        return decodeBase64(inner.getBody().getData());
                    }
                }
            }
        }

        return "";
    }


    //fetching and displaying email body for sent and coming emails
//    public String readEmailBody(String userEmail, String messageId) throws Exception {
//        try {
//            Gmail gmailService = getGmail(userEmail);
//
//            // Fetch full message details
//            Message message = gmailService.users().messages()
//                    .get("me", messageId)
//                    .setFormat("full")//full content (including headers + all message parts)
//                    .execute();
//
//            // Extract body content
//            String body = extractBodyFromMessage(message);
//            String subject = "", from = "";
//            for (MessagePartHeader header : message.getPayload().getHeaders()) {
//
//                //header.getName() gives the key like Subject, From, To, Date, etc.
//                if ("Subject".equalsIgnoreCase(header.getName())){
//                    subject = header.getValue();// gets the value of the header
//                }else if ("From".equalsIgnoreCase(header.getName())){
//                    from = header.getValue();
//                }
//            }
//
//            System.out.println("📧 From: " + from);
//            System.out.println("📝 Subject: " + subject);
//            System.out.println("📨 Body:\n" + body);
//
//            return body;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
}
