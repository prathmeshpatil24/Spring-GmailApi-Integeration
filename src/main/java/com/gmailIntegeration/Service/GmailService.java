package com.gmailIntegeration.Service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GmailService {

    @Autowired
    private Gmail gmailServiceBean;

    /**
     * Lists all labels in the user's Gmail account
     */
    public List<String> listLabels() throws IOException {
        String user = "me";
        ListLabelsResponse listResponse = gmailServiceBean.users().labels().list(user).execute();
        List<Label> labels = listResponse.getLabels();

        List<String> labelNames = new ArrayList<>();

        if (labels == null || labels.isEmpty()) {
            System.out.println("No labels found.");
            return labelNames;
        }

        System.out.println("Labels:");
        for (Label label : labels) {
            String labelName = label.getName();
            labelNames.add(labelName);
            System.out.println(labelName);
        }

        return labelNames;
    }

    /**
     * Gets a specific label by ID
     */
    public Label getLabel(String labelId) throws IOException {
        return gmailServiceBean.users().labels().get("me", labelId).execute();
    }

    /**
     * Gets the total number of labels
     */
    public int getLabelCount() throws IOException {
        ListLabelsResponse listResponse = gmailServiceBean.users().labels().list("me").execute();
        List<Label> labels = listResponse.getLabels();
        return labels != null ? labels.size() : 0;
    }

    public String getCurrentUserEmail() throws IOException {
        Gmail.Users.GetProfile profileRequest = gmailServiceBean.users().getProfile("me");
        return profileRequest.execute().getEmailAddress();
    }

    public void sendEmail(String toEmail, String subject, String bodyText) throws Exception {
        String fromEmail = getCurrentUserEmail(); // Fetch logged-in Gmail ID

        String rawEmail = "From: " + fromEmail + "\r\n" +
                "To: " + toEmail + "\r\n" +
                "Subject: " + subject + "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
                bodyText;

        Message message = new Message();
        message.setRaw(Base64.getUrlEncoder()
                .encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8)));

        Message message1 = gmailServiceBean.users().messages().send("me", message).execute();
    }

    public List<String> getInboxEmails() throws IOException {
        List<String> emailList = new ArrayList<>();

        // Fetch the first 10 messages from inbox
        ListMessagesResponse response = gmailServiceBean.users().messages().list("me")
                .setLabelIds(Collections.singletonList("INBOX"))
                .setMaxResults(10L)
                .execute();

        List<Message> messages = response.getMessages();

        if (messages == null || messages.isEmpty()) {
            emailList.add("No emails found in inbox.");
            return emailList;
        }

        for (Message msg : messages) {
            Message message = gmailServiceBean.users().messages().get("me", msg.getId()).setFormat("metadata").execute();

            String subject = "";
            String from = "";

            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                if (header.getName().equalsIgnoreCase("Subject")) {
                    subject = header.getValue();
                } else if (header.getName().equalsIgnoreCase("From")) {
                    from = header.getValue();
                }
            }

            emailList.add("📩 From: " + from + " | Subject: " + subject);
        }

        return emailList;
    }



//    public String writeGmail() {
//        gmailServiceBean.users().messages();
//    }
}
