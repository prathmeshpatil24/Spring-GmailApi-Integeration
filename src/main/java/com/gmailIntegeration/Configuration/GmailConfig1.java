package com.gmailIntegeration.Configuration;

import com.gmailIntegeration.Repo.GmailTokenRepository;
import com.gmailIntegeration.Utils.JpaDataStoreFactory;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class GmailConfig1 {

    public static final String APPLICATION_NAME = "Gmail API Spring Boot";
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Arrays.asList(
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_MODIFY
    );

    private final GmailTokenRepository repository;

    @Value("${google.credentials.file.path}")
    private String credentialsFilePath;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    public GmailConfig1(GmailTokenRepository repository) {
        this.repository = repository;
    }

    //0 - Build Google Authorization Code Flow
    public GoogleAuthorizationCodeFlow buildFlow() throws Exception{
        // Creates a secure, trusted HTTP transport channel used for making HTTPS requests to Google’s servers.
        var HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        //Loads our Google OAuth 2.0 client credentials from the classpath/app.properties file.
        InputStreamReader inputStreamReader = new InputStreamReader(
                getClass().getResourceAsStream(credentialsFilePath)
        );

        //Parses the loaded JSON into a structured GoogleClientSecrets object.
        //Google’s API libraries expect credentials in this specific object format to initiate the OAuth flow.
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, inputStreamReader);

        //Creates a custom DataStoreFactory that uses JPA to persist OAuth tokens in our database.
        DataStoreFactory dataStoreFactory = new JpaDataStoreFactory(repository);

        //Builds the GoogleAuthorizationCodeFlow object, configuring it with our HTTP transport, JSON factory,
        // client secrets, requested scopes, and the custom data store factory for token persistence.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, //For making API calls securely
                JSON_FACTORY, //For parsing/serializing JSON responses
                clientSecrets, //app’s identity (client_id, client_secret)
                SCOPES //List of permissions (read, send, modify Gmail)
        )
                .setDataStoreFactory(dataStoreFactory) //token persistence
                .setAccessType("offline") //requests a refresh token, so your app can access Gmail even when the user is offline.
                .setApprovalPrompt("force") //Google will always show the OAuth2 consent screen again for this app, even if the user has previously granted consent for dev/testing purpose.
                .build();

        return flow;// return the google authorization code flow
    }

    //1 — Generate Authorization URL for user consent
    public String getAuthorizationUrl() throws Exception {
        GoogleAuthorizationCodeFlow flow = buildFlow();// build the flow

        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build(); //generate the Authorization URL google login
    }

    //2 — Handle callback and save tokens to DB
    public Credential handleCallback(String code) throws Exception {
        var HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        InputStreamReader in = new InputStreamReader(
                getClass().getResourceAsStream(credentialsFilePath)
        );

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, in);

        DataStoreFactory dataStoreFactory = new JpaDataStoreFactory(repository);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientSecrets,
                SCOPES
        )
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        // Exchange authorization code for tokens
        var tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret(),
                code,
                redirectUri
        ).execute();

        // Temporary credential to fetch user's real Gmail address
        //flow.createAndStoreCredential(tokenResponse, "temp");
        // Create a temporary credential (NOT stored in DB)
        Credential tempCredential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientAuthentication(new ClientParametersAuthentication(
                        clientSecrets.getDetails().getClientId(),
                        clientSecrets.getDetails().getClientSecret()
                ))
                .setTokenServerEncodedUrl("https://oauth2.googleapis.com/token")
                .build()
                .setFromTokenResponse(tokenResponse);

        //Builds a Gmail service instance using the temporary credential.
        //Calls Gmail’s users().getProfile("me") endpoint.
        //Gets the real Gmail address of the authenticated user
        Gmail gmail = buildGmail(tempCredential);
        String userEmail = gmail.users().getProfile("me").execute().getEmailAddress();

        System.out.println("✅ Logged in Gmail user: " + userEmail);

        // Store tokens under real Gmail address
        // Remove temporary credential if exists
        //Creates a final Credential object and stores it persistently in your JpaDataStoreFactory (
        return flow.createAndStoreCredential(tokenResponse, userEmail);
    }

    //3 — Retrieve stored credentials anytime
    public Credential getStoredCredential(String userEmail) throws Exception {
        GoogleAuthorizationCodeFlow flow = buildFlow();
        return flow.loadCredential(userEmail);
    }

    //4 Build Gmail instance dynamically
    public Gmail buildGmail(Credential credential) throws GeneralSecurityException, IOException {
        var HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    //5 — Get Gmail service dynamically (no bean)
    public Gmail getGmailService() throws Exception {
        var allTokens = repository.findAll();
        if (allTokens.isEmpty()) {
            throw new IllegalStateException("⚠️ No Gmail tokens found. Please authorize via /api/gmail/authorize first.");
        }

        String userEmail = allTokens.get(0).getUserEmail();
        Credential credential = getStoredCredential(userEmail);

        if (credential == null) {
            throw new IllegalStateException("⚠️ No valid credential found for user: " + userEmail);
        }

        return buildGmail(credential);
    }
}

