package com.gmailIntegeration.Service;

import com.gmailIntegeration.Configuration.GmailConfig1;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.security.GeneralSecurityException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

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

    //listing inbox emails
    public List<String> getInboxEmails(String userEmail) throws Exception {

        try {

            Gmail gmailService = getGmail(userEmail);
            List<String> emailList = new ArrayList<>();

            ListMessagesResponse response = gmailService.users()
                    .messages()
                    .list("me")
                    .setLabelIds(Collections.singletonList("INBOX"))
                    .setMaxResults(10L)
                    .execute();

            //System.out.println(response.getNextPageToken());// for pagination

            List<Message> messages = response.getMessages();

            if (messages == null || messages.isEmpty()) {
                emailList.add("No emails found in inbox.");
                return emailList;
            }

            for (Message msg : messages) {
                String messageId = msg.getId();  //this is your unique ID
                Message message = gmailService.users()
                        .messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .execute();

                String subject = "", from = "";

                //Iterates through all message headers and extracts specific ones
                for (MessagePartHeader header : message.getPayload().getHeaders()) {

                    //header.getName() gives the key like Subject, From, To, Date, etc.
                    if ("Subject".equalsIgnoreCase(header.getName())){
                        subject = header.getValue();// gets the value of the header
                    } else if ("From".equalsIgnoreCase(header.getName())){
                        from = header.getValue();
                    }
                }
                emailList.add("📩 From: " + from + " | Subject: " + subject + " | ID: " + messageId);// for testing purpose
            }
            for (String emailInfo : emailList) {
                System.out.println(emailInfo);
            }
            return emailList;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    //sending email
    public void sendEmail(String userEmail, String toEmail, String subject, String bodyText) throws Exception {

       try {
           Gmail gmailService = getGmail(userEmail);

           //multiple recipients (To, Cc, Bcc) this also we can do

           String rawEmail = "From: " + userEmail + "\r\n" +
                   "To: " + toEmail + "\r\n" +
                   "Subject: " + subject + "\r\n" +
                   "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
                   bodyText;

           Message message = new Message();

           //Gmail API requires the message content to be Base64 URL-safe encoded (as per RFC 4648).
           //This encoding ensures that the email content can be safely transmitted over protocols that may not handle binary data correctly.
           //This ensures that special characters (+, /, etc.) don’t break when transmitted over HTTP.
           message.setRaw(Base64.getUrlEncoder()
                   .encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8)));

           gmailService.users().messages().send("me", message).execute();//actual API call that sends our email through Gmail’s servers.
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
    }

    //deleting email
    public void deleteEmail(String userEmail, String messageId) throws Exception {
        try {
            Gmail gmailService = getGmail(userEmail);

            //messages():- Access the messages sub-resource (emails in that account)
            //delete("me", messageId):- Deletes the specified email message from the user’s mailbox.
            //Note:
            //If the email is in Trash or Spam, Gmail may delete it permanently.
            //If it’s in the Inbox, Gmail moves it to Trash by default.
            gmailService.users().messages().delete("me", messageId).execute();

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

            //get the main content, not binary data (like attachments)
            if (mimeType.equals("text/plain") || mimeType.equals("text/html")) {
                return decodeBase64(part.getBody().getData());
            }
        }

        return "";
    }

    //fetching and displaying email body
    public String readEmailBody(String userEmail, String messageId) throws Exception {
      try {
          Gmail gmailService = getGmail(userEmail);

          // Fetch full message details
          Message message = gmailService.users().messages()
                  .get("me", messageId)
                  .setFormat("full")//full content (including headers + all message parts)
                  .execute();

          // Extract body content
          String body = extractBodyFromMessage(message);
          String subject = "", from = "";
          for (MessagePartHeader header : message.getPayload().getHeaders()) {

              //header.getName() gives the key like Subject, From, To, Date, etc.
              if ("Subject".equalsIgnoreCase(header.getName())){
                  subject = header.getValue();// gets the value of the header
              }else if ("From".equalsIgnoreCase(header.getName())){
                  from = header.getValue();
              }
          }

          System.out.println("📧 From: " + from);
          System.out.println("📝 Subject: " + subject);
          System.out.println("📨 Body:\n" + body);

          return body;
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    }

    //listing all labels in Gmail account
    public List<String> listLabels(String userEmail) throws Exception {
        try {
            Gmail gmailService = getGmail(userEmail);

            ListLabelsResponse response = gmailService.users().labels().list("me").execute();
            List<String> labelNames = new ArrayList<>();

            for (Label label : response.getLabels()) {
                labelNames.add(label.getName());
            }

            return labelNames;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
