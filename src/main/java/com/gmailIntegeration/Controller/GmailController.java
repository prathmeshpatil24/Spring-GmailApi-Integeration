package com.gmailIntegeration.Controller;


import com.gmailIntegeration.Service.GmailService;
import com.gmailIntegeration.Utils.JsonOrTextConversion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailService gmailService;
    private final JsonOrTextConversion jsonOrTextConversion;

    public GmailController(GmailService gmailService, JsonOrTextConversion jsonOrTextConversion) {
        this.gmailService = gmailService;
        this.jsonOrTextConversion = jsonOrTextConversion;
    }

    @GetMapping("/inbox/{email}")
    public ResponseEntity<?> inbox(@PathVariable String email) throws Exception {
        return ResponseEntity.ok(gmailService.getInboxEmails(email));
    }

    @GetMapping("/inbox/{userEmail}/{messageId}")
    public ResponseEntity<?> readEmailBody(
            @PathVariable String userEmail,
            @PathVariable String messageId
    ) {
        try {
            String body = gmailService.readEmailBody(userEmail, messageId);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read email: " + e.getMessage());
        }
    }

    @PostMapping("/send/{email}")
    public String send(@PathVariable String email,
                       @RequestParam String to,
                       @RequestParam String subject,
                       @RequestParam String body) throws Exception {
        try {
            gmailService.sendEmail(email, to, subject, body);
            return ResponseEntity.ok("✅ Email sent successfully! + to: " + to + "from: " + email).getBody();
        }catch (Exception e) {
            e.printStackTrace();
            return "❌ Failed to send email: " + e.getMessage();
        }
    }




    @DeleteMapping("/delete/{email}/{messageId}")
    public String delete(@PathVariable String email,
                         @PathVariable String messageId)
            throws Exception {
        gmailService.deleteEmail(email, messageId);
        return "Email deleted successfully!";
    }


    @GetMapping("/sent/{email}")
    public ResponseEntity<?> sentItems(@PathVariable String email) throws Exception {
        try {
            List<String> sentEmails = gmailService.sentEmails(email);

            return ResponseEntity.ok(sentEmails);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


//    @Autowired
//    private GmailService gmailService;
//
//    /**
//     * GET endpoint to list all Gmail labels
//     * URL: http://localhost:8080/api/gmail/labels
//     */
//    @GetMapping("/labels")
//    public ResponseEntity<?> getLabels() {
//        try {
//            List<String> labels = gmailService.listLabels();
//            return ResponseEntity.ok(labels);
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error fetching labels: " + e.getMessage());
//        }
//    }
//
//    /**
//     * GET endpoint to get a specific label by ID
//     * URL: http://localhost:8080/api/gmail/labels/{labelId}
//     */
//    @GetMapping("/labels/{labelId}")
//    public ResponseEntity<?> getLabel(@PathVariable String labelId) {
//        try {
//            Label label = gmailService.getLabel(labelId);
//            return ResponseEntity.ok(label);
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error fetching label: " + e.getMessage());
//        }
//    }
//
//    /**
//     * GET endpoint to get the count of labels
//     * URL: http://localhost:8080/api/gmail/labels/count
//     */
//    @GetMapping("/labels/count")
//    public ResponseEntity<?> getLabelCount() {
//        try {
//            int count = gmailService.getLabelCount();
//            return ResponseEntity.ok("Total labels: " + count);
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error counting labels: " + e.getMessage());
//        }
//    }
//
//    @GetMapping("/currentUser")
//    public ResponseEntity<String> getCurrentUser() throws IOException {
//        String currentUserEmail = gmailService.getCurrentUserEmail();
//        return ResponseEntity.ok("Logged in as: " + currentUserEmail);
//    }
//
//    @PostMapping("/sendEmail")
//    public ResponseEntity<?> sendEmail(
//            @RequestParam String to,
//            @RequestParam String subject,
//            @RequestParam String body) {
//        try {
//            gmailService.sendEmail(to, subject, body);
//            return ResponseEntity.status(HttpStatus.OK).body("Email sent successfully to: " + to + "From logged-in user." + gmailService.getCurrentUserEmail());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email: " + e.getMessage());
//        }
//    }
//
//    @GetMapping("/inbox")
//    public List<String> getInbox() {
//        try {
//            return gmailService.getInboxEmails();
//        } catch (Exception e) {
//            return List.of("❌ Error fetching inbox: " + e.getMessage());
//        }
//    }
