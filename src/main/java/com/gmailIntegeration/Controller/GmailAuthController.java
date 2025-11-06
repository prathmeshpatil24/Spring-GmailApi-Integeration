package com.gmailIntegeration.Controller;


import com.gmailIntegeration.Configuration.GmailConfig1;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class GmailAuthController {

    private final GmailConfig1 gmailConfig;

    public GmailAuthController(GmailConfig1 gmailConfig) {
        this.gmailConfig = gmailConfig;
    }

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestParam("email") String email) throws Exception {

       try {
           boolean authorized = gmailConfig.isAuthorized(email);// Check if already authorized

           if (authorized) {
               return ResponseEntity.ok("✅ Gmail already authorized for " + email +
                       ". You can use Gmail API for read and write.");
           }
           String url = gmailConfig.getAuthorizationUrl();
           return ResponseEntity.ok("<a href=\"" + url + "\">Authorize Gmail Access</a>");
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> oauthCallback(@RequestParam("code") String code) throws Exception {
         try {
             Credential credential = gmailConfig.handleCallback(code);
             Gmail gmail = gmailConfig.buildGmail(credential);

             Gmail.Users.GetProfile getProfile = gmail.users().getProfile("me");// Test API call

             String emailAddress = getProfile.execute().getEmailAddress();
             
             System.out.println("Authorized Gmail ID: " + emailAddress);

             return ResponseEntity.ok( "✅ Gmail authorization successful! Tokens saved to DB. + \n" +
                         "Authorized Gmail ID: " + emailAddress);

         }catch (Exception exception){
            return ResponseEntity.ok(exception.getMessage());
         }

    }
}
