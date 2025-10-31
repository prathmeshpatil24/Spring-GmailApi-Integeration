package com.gmailIntegeration.Configuration;

import com.gmailIntegeration.Repo.GmailTokenRepository;
import com.gmailIntegeration.Utils.JpaDataStoreFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class GmailConfig {
    public static final String APPLICATION_NAME = "Gmail API Spring Boot";
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Arrays.asList(
            GmailScopes.GMAIL_READONLY,   // to read messages
            GmailScopes.GMAIL_SEND,       // to send emails
            GmailScopes.GMAIL_MODIFY      // to manage labels, mark read/unread, etc.
    );
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    @Autowired
    private GmailTokenRepository gmailTokenRepository;

    /**
     * Creates an authorized Credential object.
     */
 //   public Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets
//        InputStream in = GmailConfig.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
//        if (in == null) {
//            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
//        }
//        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
//
//        // Print the full tokens path here 👇
////        System.out.println("Token folder path: " + new java.io.File(TOKENS_DIRECTORY_PATH).getAbsolutePath());
//
//        // Build flow and trigger user authorization request
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                HTTP_TRANSPORT,
//                JSON_FACTORY,
//                clientSecrets,
//                SCOPES)
//               // .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
//                .setDataStoreFactory(new JpaDataStoreFactory(gmailTokenRepository))// custom data store factory
//                .setAccessType("offline")
//                .setApprovalPrompt("force")
//                .build();
//
//        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
//                .setPort(8080)
//                .setCallbackPath("/oauth2/callback")
//                .build();
//        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
//    }
//
//        @Bean
//    public Gmail gmailServiceBean() throws GeneralSecurityException, IOException {
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//
//        //and inside that method, it automatically builds and launches the OAuth flow —
//            // so it prints or opens this in console:
//        //Please open the following address in your browser:
//            //https://accounts.google.com/o/oauth2/auth?access_type=offline&approval_prompt=force...
//        Credential credential = getCredentials(HTTP_TRANSPORT);
//
//        Gmail built = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//        return built;
//    }

}


