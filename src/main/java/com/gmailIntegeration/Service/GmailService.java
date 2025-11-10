package com.gmailIntegeration.Service;

import com.gmailIntegeration.Configuration.GmailConfig1;

import com.gmailIntegeration.exceptions.UnauthorizedUserException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
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
import java.util.stream.Collectors;

@Service
public class GmailService {

    private final GmailConfig1 gmailConfig;

    public GmailService(GmailConfig1 gmailConfig) {
        this.gmailConfig = gmailConfig;
    }

    // helper method to create Gmail instance
    public Gmail getGmail(String userEmail) throws Exception {
        try{
            Credential credential = gmailConfig.getStoredCredential(userEmail);
            if (credential == null) {
                String authorizationUrl= gmailConfig.getAuthorizationUrl();
                System.out.println("authorized url:- " +  authorizationUrl);
                throw new UnauthorizedUserException("No credential found for user: " + userEmail,
                        "Please authenticate using the following URL: " +
                        authorizationUrl);
            }
            return new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("Gmail API Spring Boot").build();

        } catch (IOException e) {
            throw new IOException("Failed to load Gmail credentials: " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new GeneralSecurityException("Security error initializing Gmail service: " + e.getMessage(), e);
        } catch (UnauthorizedUserException e) {
            throw e; // let controller handle this
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public Map<String,Object> currentUserProfile(String userEmail) throws Exception {
        Gmail gmail = getGmail(userEmail);

        Map<String,Object> profileInfo = new HashMap<>();
        try {
            Profile profile = gmail.users().getProfile("me").execute();

            for (Map.Entry<String, Object> entry : profile.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                profileInfo.put(key,value);
            }

            //logging profile info
            System.out.println("User Profile Information:");
            profileInfo.forEach(
                    (k,v) -> System.out.println(k + ": " + v)
            );

            return profileInfo;

        } catch (UnauthorizedUserException e) {
            // Re-throw it so controller can return auth URL
            throw e;
        } catch (IOException e) {
            throw new IOException("Failed to fetch user profile: " + e.getMessage(), e);

        }catch (Exception e) {
            throw new RuntimeException("Unexpected error while fetching user profile: " + e.getMessage(), e);
        }
    }

    //listing all labels in Gmail account
    public Map<String,String> labelList(String userEmail) throws Exception {

        Gmail gmail = getGmail(userEmail);

        Map<String,String> labelInfo = new HashMap<>();
        try {

            ListLabelsResponse response = gmail.users()
                    .labels()
                    .list("me")
                    .execute();

            for (Label label : response.getLabels()) {
                labelInfo.put(label.getId(),label.getName());
            }
            //logging label info
            labelInfo.forEach(
                    (k,v) ->
                            System.out.println("Label ID: " + k + " | Label Name: " + v)
            );

            return labelInfo;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch labels: " + e.getMessage());
        }
    }


    //listing emails by label dynamically
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

            for(Message msg : messages) {
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

                                Map<String, Object> emailInfo = new HashMap<>();

                                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                                     //header.getName() gives the key like Subject, From, To, Date,  etc.
                                    //header.getValue()  gets the value of the header
                                    emailInfo.put(header.getName(), header.getValue());
                                }
                                //Add extra useful details (outside headers)
                                emailInfo.put("MessageId", message.getId());
                                emailInfo.put("ThreadId", message.getThreadId());

                                // Optional: snippet or label info
                                emailInfo.put("Snippet", message.getSnippet());
                                emailInfo.put("LabelIds", message.getLabelIds());

                                emailsList.add(emailInfo);
                            }

                            @Override
                            public void onFailure(GoogleJsonError googleJsonError, HttpHeaders responseHeaders) {
                                System.err.println("Failed to fetch message: " + googleJsonError.getMessage());
                                Map<String, Object> errorInfo = new HashMap<>();
                                errorInfo.put("error", true);
                                errorInfo.put("message", "Failed to fetch message.");
                                errorInfo.put("details", googleJsonError.getMessage());
                                emailsList.add(errorInfo);

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
    public List<Map<String, Object>> getInboxEmailList(String userEmail) throws Exception {
        Gmail gmail = getGmail(userEmail);
        List<Map<String, Object>> emailsList = Collections.synchronizedList(new ArrayList<>());
        try {

            ListMessagesResponse response = gmail.users()
                    .messages()
                    .list("me")
                    .setLabelIds(Arrays.asList(
                            "INBOX",
                            "CATEGORY_PERSONAL"
                    ))
                    .setIncludeSpamTrash(false)
                    .setPrettyPrint(true)
                    .setMaxResults(10L)
                    .execute();

            //System.out.println(response.getNextPageToken());// for pagination

            List<Message> messages = response.getMessages();

            if (messages == null || messages.isEmpty()) {
                Map<String, Object> noEmailMap = new HashMap<>();
                noEmailMap.put("message", "No emails found in inbox.");
                emailsList.add(noEmailMap);

                //emailList.add("No emails found in inbox.");
                return emailsList;
            }

            BatchRequest batch = gmail.batch();

            for (Message msg : messages) {
                String messageId = msg.getId();  //this is your unique ID
                gmail.users()
                        .messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .queue(batch, new JsonBatchCallback<Message>() {
                            @Override
                            public void onFailure(GoogleJsonError googleJsonError, HttpHeaders httpHeaders) throws IOException {
                                System.err.println("Failed to fetch message: " + googleJsonError.getMessage());
                                Map<String, Object> errorInfo = new HashMap<>();
                                errorInfo.put("error", true);
                                errorInfo.put("message", "Failed to fetch message.");
                                errorInfo.put("details", googleJsonError.getMessage());
                                emailsList.add(errorInfo);
                            }

                            @Override
                            public void onSuccess(Message message, HttpHeaders httpHeaders) throws IOException {
                                Map<String, Object> emailInfo = new HashMap<>();

                                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                                    //header.getName() gives the key like Subject, From, To, Date,  etc.
                                    //header.getValue()  gets the value of the header
                                    emailInfo.put(header.getName(), header.getValue());
                                }
                                //Add extra useful details (outside headers)
                                emailInfo.put("MessageId", message.getId());
                                emailInfo.put("ThreadId", message.getThreadId());

                                // Optional: snippet or label info
                                emailInfo.put("Snippet", message.getSnippet());
                                emailInfo.put("LabelIds", message.getLabelIds());

                                emailsList.add(emailInfo);
                            }
                        });
            }
            batch.execute();

            emailsList.forEach(email -> {
                System.out.println(email);
                System.out.println("-----------------------------------");
            });
            return emailsList;
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
                //If isStarred is true → star (star label)
                mod.setAddLabelIds(Collections.singletonList("STARRED"));

                // mod.setAddLabelIds(Arrays.asList(category, "STARRED"));// for when category filed will be dynamic, pass it from controller
                System.out.println("starring email... " + messageId);
            } else {
                //If already unstar → star (add label)
                mod.setRemoveLabelIds(Collections.singletonList("STARRED"));
                System.out.println("Un-Starring email... " + messageId);
            }

            gmail.users()
                    .messages()
                    .modify("me", messageId, mod)
                    .execute();

            System.out.println("Toggle complete for message: " + messageId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to toggle star for message: " + messageId, e);
        }
    }

    //listing starred emails
    public List<Map<String,Object>> getStarredEmailList(String userEmail) throws Exception{
        Gmail gmail = getGmail(userEmail);
        ArrayList<Map<String,Object>>staredEmailList = new ArrayList<>();

        try {

            ListMessagesResponse response = gmail.users().messages()
                    .list("me")
//                    .setLabelIds(Collections.singletonList("STARRED"))
                    .setQ("label:inbox label:starred")  //only starred mails in inbox
                    .setMaxResults(20L)
                    .execute();

            List<Message> starredMessages = response.getMessages();

            if (starredMessages == null || starredMessages.isEmpty()) {
                //logging
                System.out.println("No starred emails found.");
                Map<String,Object> noStarredEmailMap = new HashMap<>();
                noStarredEmailMap.put("message","No starred emails found.");
                staredEmailList.add(noStarredEmailMap);

                return staredEmailList;
            }

//            for (Message message : starredMessages) {
//                System.out.println("Starred Email ID: " + message.getId());
//            }

            Map<String, Object> emailInfo = new HashMap<>();
            for (Message msg : starredMessages) {
                String messageId = msg.getId();  //this is your unique ID
                Message message = gmail.users()
                        .messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .execute();
                //here also we can add batch for increase api response speed

                //Iterates through all message headers and extracts specific ones
                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                    emailInfo.put(header.getName(),header.getValue());
                }

                //Add extra useful details (outside headers)
                emailInfo.put("MessageId", message.getId());
                emailInfo.put("ThreadId", message.getThreadId());

                // Optional: snippet or label info
                emailInfo.put("Snippet", message.getSnippet());
                emailInfo.put("LabelIds", message.getLabelIds());
            }

            staredEmailList.add(emailInfo);
            //logging
            for (Map<String,Object> starredEmail:staredEmailList){
                System.out.println(starredEmail);
                System.out.println("----------------------------------");
            }

            System.out.println(staredEmailList.size());

            return staredEmailList;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch starred emails: " + e.getMessage(), e);
        }
    }

    //sending email without attachment
    //Note:- this method is for sending one mail for sending mail to multi user we need to apply for loop
    public void sendEmailWithoutAttachment(String userEmail,
                          String toEmail,
                          String bcc,
                          String cc,
                          String subject,
                          String bodyText) throws Exception {

       try {
           Gmail gmail = getGmail(userEmail);
//          Properties properties = new Properties();
           //multiple recipients (To, Cc, Bcc) this also we can do

           StringBuilder rawEmailBuilder = new StringBuilder();
           rawEmailBuilder.append("From: ").append(userEmail).append("\r\n");
           rawEmailBuilder.append("To: ").append(toEmail).append("\r\n");

           if (cc != null && !cc.isBlank()) {
               rawEmailBuilder.append("Cc: ").append(cc).append("\r\n");
           }
           if (bcc != null && !bcc.isBlank()) {
               rawEmailBuilder.append("Bcc: ").append(bcc).append("\r\n");
           }

           rawEmailBuilder.append("Subject: ").append(subject).append("\r\n");
           rawEmailBuilder.append("Content-Type: text/plain; charset=utf-8\r\n\r\n");
           rawEmailBuilder.append(bodyText);

           String rawEmail = rawEmailBuilder.toString();

           Message message = new Message();

           //Gmail API requires the message content to be Base64 URL-safe encoded (as per RFC 4648).
           //This encoding ensures that the email content can be safely transmitted over protocols that may not handle binary data correctly.
           //This ensures that special characters (+, /, etc.) don’t break when transmitted over HTTP.
           message.setRaw(Base64.getUrlEncoder()
                   .encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8)));

           gmail.users().messages().send("me", message).execute();//actual API call that sends our email through Gmail’s servers.
       } catch (Exception e) {
           throw new RuntimeException("Failed to send email:- " + e.getMessage(), e);
       }
    }

    //here we can send  same mail to multiple mailIDs
    public void sendEmailWithAttachment(String userEmail,
                                        List<String> toEmails,
                                        List<String> CC,
                                        List<String> BCC,
                                        String subject,
                                        String bodyText,
                                        List<MultipartFile> attachmentFiles) throws Exception {

        Gmail gmail = getGmail(userEmail);

       try {
           Properties props = new Properties();
           Session session = Session.getDefaultInstance(props, null);

           MimeMessage email = new MimeMessage(session);

           email.setFrom(new InternetAddress(userEmail));
           for (String toEmail: toEmails){
               System.out.println("sending mail at one time to multiple mailId:- " + toEmail);
               email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(toEmail));
           }
           System.out.println("-----------------------");

//           // Optional: add CC recipients
           if (CC != null && !CC.isEmpty()) {
               for (String cc: CC){
                   System.out.println("CC mail list:- " + cc);
               email.addRecipient(jakarta.mail.Message.RecipientType.CC, new InternetAddress(cc));
               }
               System.out.println("-----------------------");
           }

//           // Optional: add BCC recipients
           if (BCC != null && !BCC.isEmpty()) {
               for (String bcc: BCC){
                   System.out.println("BCC mail list:- " + bcc);
               email.addRecipient(jakarta.mail.Message.RecipientType.BCC, new InternetAddress(bcc));
               }
               System.out.println("------------------------");
           }

           email.setSubject(subject, "UTF-8");

           MimeBodyPart textPart = new MimeBodyPart();
           textPart.setText(bodyText, "UTF-8");

           MimeMultipart multipart = new MimeMultipart();
           multipart.addBodyPart(textPart);

           //Handle optional MultipartFile safely
           if (attachmentFiles!=null && !attachmentFiles.isEmpty()){
               for (MultipartFile attachmentFile:attachmentFiles){
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
               }
               System.out.println("Attachment files list is null or empty.");
           }else {
               System.out.println("No attachment provided. Sending email without attachment.");
           }

           email.setContent(multipart);

           //--- Encode message ---
           ByteArrayOutputStream buffer = new ByteArrayOutputStream();
           email.writeTo(buffer);

           String encodedEmail = Base64.getUrlEncoder()
                   .encodeToString(buffer.toByteArray());

           Message message = new Message();
           message.setRaw(encodedEmail);

           // --- Send email via Gmail API ---
           Message sentMessage = gmail.users()
                   .messages()
                   .send("me", message)
                   .execute();

           if (sentMessage!=null && sentMessage.getId()!=null){
               if (attachmentFiles!=null && !attachmentFiles.isEmpty()){
                   String filesName = attachmentFiles.stream()
                           .map(MultipartFile::getOriginalFilename)
                           .collect(Collectors.joining(","));
                     System.out.println("Email sent successfully with attachments: " + filesName);
               }else {
                   System.out.println("Email sent successfully without attachments.");
               }
           }else {
               System.err.println("Email sending failed: Gmail API returned null response.");
           }

       } catch (Exception e) {
           throw new RuntimeException("Error while sending email: " + e.getMessage());
       }
    }

    //listing sent emails
    public List<Map<String, Object>> sentEmailList(String userEmail) throws Exception{
        Gmail gmail = getGmail(userEmail);

        //wraps your ArrayList in a thread-safe wrapper, meaning:
        //Only one thread can modify it at a time (add, remove, etc.)
        //Prevents concurrent modification problems.
        //That’s why it’s used here — because Gmail’s batch callbacks run asynchronously.
        List<Map<String, Object>> emailsList = Collections
                .synchronizedList(new ArrayList<>());
        try {
            ListMessagesResponse listOfSentEmails = gmail.users()
                    .messages()
                    .list("me")
                    .setLabelIds(Arrays.asList("SENT"))
                    .setMaxResults(20L)
                    .execute();

            List<Message> messages = listOfSentEmails.getMessages();


            if (messages == null || messages.isEmpty()){
                Map<String, Object> noEmailMap = new HashMap<>();
                noEmailMap.put("message", "No emails found in inbox.");
                emailsList.add(noEmailMap);
                return emailsList;
            }

            BatchRequest batch = gmail.batch();

            for (Message message : messages) {
                String messageId = message.getId();
                gmail
                        .users()
                        .messages()
                        .get("me", messageId)
                        .setFormat("metadata")
                        .queue(batch, new JsonBatchCallback<Message>() {
                            @Override
                            public void onFailure(GoogleJsonError googleJsonError, HttpHeaders httpHeaders) throws IOException {
                                System.err.println("Failed to fetch message: " + googleJsonError.getMessage());
                                Map<String, Object> errorInfo = new HashMap<>();
                                errorInfo.put("error", true);
                                errorInfo.put("message", "Failed to fetch message.");
                                errorInfo.put("details", googleJsonError.getMessage());
                                emailsList.add(errorInfo);
                            }

                            @Override
                            public void onSuccess(Message message, HttpHeaders httpHeaders) throws IOException {
                                Map<String, Object> emailInfo = new HashMap<>();

                                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                                    //header.getName() gives the key like Subject, From, To, Date,  etc.
                                    //header.getValue()  gets the value of the header
                                    emailInfo.put(header.getName(), header.getValue());
                                }
                                //Add extra useful details (outside headers)
                                emailInfo.put("MessageId", message.getId());
                                emailInfo.put("ThreadId", message.getThreadId());

                                // Optional: snippet or label info
                                emailInfo.put("Snippet", message.getSnippet());
                                emailInfo.put("LabelIds", message.getLabelIds());

                                emailsList.add(emailInfo);
                            }
                        });
            }
            batch.execute();
            // Step 4: Print summary
            emailsList.forEach(email -> {
                System.out.println(email);
                System.out.println("-----------------------------------");
            });

            return emailsList;

        }catch (Exception ex){
            throw new RuntimeException("Failed to fetch sent emails: " + ex.getMessage(), ex);
        }
    }

    //listing draft emails
    public List<Map<String,Object>> draftEmailList(String userEmails) throws Exception{
        Gmail gmail = getGmail(userEmails);

        List<Map<String, Object>> draftMailList = Collections
                .synchronizedList(new ArrayList<>());

        try {
            ListDraftsResponse listDraftsResponse = gmail.users()
                    .drafts()
                    .list("me")
                    .setMaxResults(10L)
                    .execute();

            if (listDraftsResponse == null || listDraftsResponse.isEmpty()){

                Map<String,Object> noDraftEmailMap = new HashMap<>();
                System.out.println("No draft emails found.");

                noDraftEmailMap.put("message","No draft emails found.");
                draftMailList.add(noDraftEmailMap);

                return draftMailList;
            }

            // Step 2: Create a BatchRequest to fetch all message metadata in parallel
            BatchRequest batch = gmail.batch();

            for (Draft draft : listDraftsResponse.getDrafts()) {
                String draftId = draft.getId();
                 gmail.users()
                        .drafts()
                        .get("me", draftId)
                        .setFormat("metadata")
                        .queue(batch, new JsonBatchCallback<Draft>() {
                            @Override
                            public void onFailure(GoogleJsonError googleJsonError, HttpHeaders httpHeaders) throws IOException {
                                System.err.println("Failed to fetch message: " + googleJsonError.getMessage());
                                Map<String, Object> errorInfo = new HashMap<>();
                                errorInfo.put("error", true);
                                errorInfo.put("message", "Failed to fetch message.");
                                errorInfo.put("details", googleJsonError.getMessage());
                                draftMailList.add(errorInfo);
                            }

                            @Override
                            public void onSuccess(Draft draft, HttpHeaders httpHeaders) throws IOException {
                                Map<String, Object> draftInfo = new HashMap<>();

                                for (MessagePartHeader header : draft.getMessage().getPayload().getHeaders()) {
                                    //header.getName() gives the key like Subject, From, To, Date,  etc.
                                    //header.getValue()  gets the value of the header
                                    draftInfo.put(header.getName(), header.getValue());
                                }
                                //Add extra useful details (outside headers)
                                draftInfo.put("MessageId", draft.getId());
                                draftInfo.put("ThreadId", draft.getMessage().getThreadId());

                                // Optional: snippet or label info
                                draftInfo.put("Snippet", draft.getMessage().getSnippet());
                                draftInfo.put("LabelIds", draft.getMessage().getLabelIds());

                                draftMailList.add(draftInfo);
                            }
                        });
            }

            // Step 3: Execute all requests in one batch call
            batch.execute();

            draftMailList.forEach(
                    draftEmail -> {
                        System.out.println(draftEmail);
                        System.out.println("-----------------------------------");
                    }
            );

            System.out.println("Total Draft Emails: " + draftMailList.size());
            System.out.println("-----------------------------------");

            return draftMailList;

        }catch (Exception ex){
            throw new RuntimeException("Failed to fetch draft emails: " + ex.getMessage(), ex);
        }
    }

    //listing spam emails
    public List<Map<String, Object>> spamEmailList(String userEmail) throws Exception{
        Gmail gmail = getGmail(userEmail);

        List<Map<String, Object>> spamMailList = Collections
                .synchronizedList(new ArrayList<>());

        try {

            ListMessagesResponse listMessagesResponse = gmail.users()
                    .messages()
                    .list("me")
                    .setLabelIds(Collections.singletonList("SPAM"))
                    .setIncludeSpamTrash(true)//Includes SPAM
                    .setPrettyPrint(true)
                    .setMaxResults(20L)
                    .execute();


            List<Message> listOfSpamMessages = listMessagesResponse.getMessages();

            if (listOfSpamMessages == null || listOfSpamMessages.isEmpty()) {
                Map<String,Object> noSpamInfo = new HashMap<>();
                System.out.println("No spam emails in Spam");
                noSpamInfo.put("message", "NO spam emails in Spam");

                spamMailList.add(noSpamInfo);
                return spamMailList;
            }

            BatchRequest batch = gmail.batch();
            for (Message message:listOfSpamMessages){
                String messageId = message.getId();  //this is your unique ID
                gmail.users()
                        .messages()
                        .get("me", message.getId())
                        .setFormat("metadata")
                        .queue(batch, new JsonBatchCallback<Message>() {
                            @Override
                            public void onFailure(GoogleJsonError googleJsonError, HttpHeaders httpHeaders) throws IOException {
                                System.err.println("Failed to fetch message: " + googleJsonError.getMessage());
                                Map<String, Object> errorInfo = new HashMap<>();
                                errorInfo.put("error", true);
                                errorInfo.put("message", "Failed to fetch message.");
                                errorInfo.put("details", googleJsonError.getMessage());

                                spamMailList.add(errorInfo);
                            }

                            @Override
                            public void onSuccess(Message message, HttpHeaders httpHeaders) throws IOException {
                                Map<String, Object> emailInfo = new HashMap<>();
                                     for (MessagePartHeader header:message.getPayload().getHeaders()){
                                         emailInfo.put(header.getName(), header.getName());
                                     }

                                //Add extra useful details (outside headers)
                                emailInfo.put("MessageId", message.getId());
                                emailInfo.put("ThreadId", message.getThreadId());

                                // Optional: snippet or label info
                                emailInfo.put("Snippet", message.getSnippet());
                                emailInfo.put("LabelIds", message.getLabelIds());

                                spamMailList.add(emailInfo);

                            }
                        });

            }

            batch.execute();

           //logging
            spamMailList.forEach((spamMails)->{
                System.out.println(spamMails);
                System.out.println("-----------------------------");
            });

            System.out.println("Total Spam Emails: " + spamMailList.size());
            System.out.println("-----------------------------------");

            return spamMailList;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch spam emails:- " + e.getMessage(), e);
        }
    }

     //fetching and displaying draft email body without attachment
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
    public String moveEmailToTrash(String userEmail, String messageId) throws Exception {
        Gmail gmail = getGmail(userEmail);
        try {
            Message message = gmail.users()
                    .messages()
                    .trash("me", messageId)
                    .execute();

            //  Verify that the message is in TRASH
            if (message.getLabelIds() != null && message.getLabelIds().contains("TRASH")) {
                System.out.println(" Email moved to Trash successfully!");
                return "Email moved to Trash successfully!";
            } else {
                System.out.println("Email not marked as Trash. Please check Gmail labels.");
                return "Email not marked as Trash. Please check Gmail labels.";
            }

        } catch (GoogleJsonResponseException e) {
            System.err.println("Gmail API Error while moving to Trash: " + e.getDetails().getMessage());
            throw new RuntimeException("Gmail API Error while moving email to Trash: " + e.getDetails().getMessage());
        } catch (Exception e) {
            System.err.println("Failed to move email to Trash: " + e.getMessage());
            throw new RuntimeException("Failed to move email to Trash: " + e.getMessage());
        }
    }

    //untrash email
    public String unTrashEmail(String userEmail, String messageId) throws Exception {
        Gmail gmail = getGmail(userEmail);
        try {
            Message message = gmail.users()
                    .messages()
                    .untrash("me", messageId)
                    .execute();
            //  Verify that the message is in TRASH
            if (message.getLabelIds() != null && !message.getLabelIds().contains("TRASH")) {

                    String restoredLabels = String.join(", ", message.getLabelIds());
                    System.out.println(" Email successfully moved back to " + restoredLabels);

                    return " Email successfully moved back to " + restoredLabels;

            } else {
                System.out.println("Email not marked as Trash. Please check Gmail labels.");

                return "Email not marked as Trash. Please check Gmail labels.";
            }

        } catch (GoogleJsonResponseException e) {
            System.err.println("Gmail API Error while moving to Trash: " + e.getDetails().getMessage());
            throw new RuntimeException("Gmail API Error while moving email to Trash: " + e.getDetails().getMessage());
        } catch (Exception e) {
            System.err.println("Failed to move email to Trash: " + e.getMessage());
            throw new RuntimeException("Failed to move email to Trash: " + e.getMessage());
        }
    }

    //deleting email permanently
    public String deleteEmail(String userEmail, String messageId) throws Exception {

        Gmail gmail = getGmail(userEmail);

            //messages():- Access the messages sub-resource (emails in that account)
            //delete("me", messageId):- Deletes the specified email message from the user’s mailbox.
            //Note:
            //If the email is in Trash or Spam, Gmail may delete it permanently.
            //If it’s in the Inbox, Gmail moves it to Trash by default.
             gmail.users()
                    .messages()
                    .delete("me", messageId)
                    .execute();
        try {
            //fetch the message again to confirm deletion
            gmail.users().messages().get("me", messageId).execute();
            return "Message still exists. Could not delete permanently.";
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                return "Message permanently deleted.";
            }
            throw e;
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

            // Check for HTML part
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

    //fetching full email body with headers, body and attachments
    public Map<String,Object>readFullEmailBody(String userEmail,
                                               String messageId,
                                               boolean isDraft) throws Exception{

        Gmail gmail = getGmail(userEmail);
        try {

            //fetch message or draft based on isDraft flag
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
            Map<String, String> headers = extractHeaders(message.getPayload());

            // Extract body
            String body = extractBodyFromMessage(message);

            //extract attachments
            List<Map<String, Object>> attachments = readAttachments(gmail, userEmail, message);

            //combine all email details
            Map<String,Object> emailDetails = new HashMap<>();
            emailDetails.put("to", headers.getOrDefault("To", ""));
            emailDetails.put("from", headers.getOrDefault("From", ""));
            emailDetails.put("subject", headers.getOrDefault("Subject", ""));
            emailDetails.put("date", headers.getOrDefault("Date", ""));
            emailDetails.put("body", body);
            emailDetails.put("attachments", attachments);

            // Display email details in console
            System.out.println("📧 From: " + headers.getOrDefault("From", ""));
            System.out.println("📧 Subject: " + headers.getOrDefault("Subject", ""));
            System.out.println("📬 To: " + headers.getOrDefault("To", ""));
            System.out.println("📅 Date: " + headers.getOrDefault("Date", ""));
            System.out.println("📨 Body: " + body);
            System.out.println("📎 Attachments: " + attachments.size());

            return emailDetails;// temp

        } catch (Exception e) {
            throw new RuntimeException("Failed to read email: " + e.getMessage(), e);
        }

    }

    // Extracts email headers (To, From, Subject, Date, etc.)
    private Map<String, String> extractHeaders(MessagePart payload) {
        Map<String, String> headers = new HashMap<>();
        if (payload.getHeaders() == null) return headers;

        for (MessagePartHeader header : payload.getHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
        return headers;
    }

    // Reads all attachments from an email message.
    private List<Map<String,Object>> readAttachments(Gmail gmail,
                                                     String userEmail,
                                                     Message message) throws Exception{

        List<Map<String,Object>> attachments = new ArrayList<>();

        MessagePart payload = message.getPayload();

        if (payload==null || payload.isEmpty()){
            return attachments;
        }

        for (MessagePart part:payload.getParts()){
            // Identify attachments by filename
            if (part.getFilename() != null && !part.getFilename().isEmpty()) {
                attachments.add(fetchAttachment(gmail, userEmail, message.getId(), part));
            }

            // Handle nested attachments
            if (part.getParts() != null && !part.getParts().isEmpty()) {
                attachments.addAll(readNestedAttachments(gmail, userEmail, message.getId(), part.getParts()));
            }
        }

        return attachments;
    }

    // Reads attachments nested under multipart/mixed or other structures
    private List<Map<String, Object>> readNestedAttachments(Gmail gmail, String userEmail, String messageId, List<MessagePart> parts) throws Exception {
        List<Map<String, Object>> attachments = new ArrayList<>();

        for (MessagePart inner : parts) {
            if (inner.getFilename() != null && !inner.getFilename().isEmpty()) {
                attachments.add(fetchAttachment(gmail, userEmail, messageId, inner));
            }
        }

        return attachments;
    }

    //Helper method to fetch and decode a single attachment
    private Map<String, Object> fetchAttachment(Gmail gmail, String userEmail, String messageId, MessagePart part) throws Exception {
        String attachmentId = part.getBody().getAttachmentId();

        MessagePartBody attachPart = gmail.users()
                .messages()
                .attachments()
                .get(userEmail, messageId, attachmentId)
                .execute();

        byte[] fileBytes = Base64.getUrlDecoder().decode(attachPart.getData());

        Map<String, Object> fileData = new HashMap<>();
        System.out.println("Attachment id:- " + attachmentId);
//        fileData.put("attachmentId", attachmentId);
        fileData.put("fileName", part.getFilename());
        fileData.put("mimeType", part.getMimeType());
//        fileData.put("size", fileBytes.length);
//        fileData.put("data", fileBytes);
        String viewUrl = "http://localhost:8080/api/gmail/" + userEmail + "/" + messageId + "/" + attachmentId;

        fileData.put("viewUrl", viewUrl);


        return fileData;
    }

}
