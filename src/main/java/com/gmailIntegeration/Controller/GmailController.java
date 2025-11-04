package com.gmailIntegeration.Controller;


import com.gmailIntegeration.Service.GmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailService gmailService;
//    private final JsonOrTextConversion jsonOrTextConversion;

    public GmailController(GmailService gmailService) {
        this.gmailService = gmailService;}

    @GetMapping("/currentUser/{email}")
    public ResponseEntity<String> getCurrentUser(@PathVariable String email) {
        try {
            List<String> currentUserProfile = gmailService.currentUserProfile(email);
            return ResponseEntity.ok("Logged in as: " + currentUserProfile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/labels/{email}")
    public ResponseEntity<?> getLabels(@PathVariable String email) {
            try {
                List<String> labels = gmailService.listLabels(email);
                return ResponseEntity.ok(labels);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    @GetMapping("/emails/{label}")
    public ResponseEntity<?> getEmailsByLabel(
            @PathVariable String label,
            @PathVariable String email
    ) {
        try {
            List<Map<String, Object>> emails = gmailService.getEmailsByLabel(email, label);
            return ResponseEntity.ok(emails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch emails for label: " + e.getMessage());
        }
    }

    @GetMapping("/inbox/{email}")
    public ResponseEntity<?> inbox(@PathVariable String email) throws Exception {
        try{
            return ResponseEntity.ok(gmailService.getInboxEmails(email));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/inbox/{email}/{messageId}")
    public ResponseEntity<?> readEmailBody(
            @PathVariable String email,
            @PathVariable String messageId,
            @PathVariable(required = false) boolean isDraft
    ) {
        try {
            Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);

            return ResponseEntity.ok(emailBody);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read email: " + e.getMessage());
        }
    }


    @PostMapping("/inbox/{email}/{messageId}/star")
    public ResponseEntity<?>toggleStarredEmail(
            @PathVariable String email,
            @PathVariable String messageId,
            @RequestParam boolean starStatus
    ) {
        try {
            gmailService.toggleStar(email, messageId, starStatus);
            return ResponseEntity.ok(
                    "Email " + (starStatus ? "starred" : "un-starred") + " successfully!"
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update star status: " + e.getMessage());
        }
    }

    @GetMapping("/starred/{email}")
    public ResponseEntity<?> listOfStarredEmail(@PathVariable String email){
        try{
            List<String> starredEmails = gmailService.getStarredEmails(email);
            return ResponseEntity.ok(starredEmails);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/send/{email}")
    public String send(@PathVariable String email,
                       @RequestParam String to,
                       @RequestParam String subject,
                       @RequestParam String body) throws Exception {
        try {
            gmailService.sendEmail(email, to, subject, body);
            return ResponseEntity.ok("Email sent successfully! + to: " + to + "from: " + email).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send email: " + e.getMessage();
        }
    }

    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmail(
            @RequestParam String userEmail,
            @RequestParam String toEmail,
//            @RequestParam String CC,
//            @RequestParam String BCC,
            @RequestParam String subject,
            @RequestParam String bodyText,
            @RequestParam(required = false) MultipartFile attachmentFile) {

        try {
            gmailService.sendEmailWithAttachment(userEmail, toEmail, subject, bodyText, attachmentFile);
            return ResponseEntity.ok("Email sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send email: " + e.getMessage());
        }
    }


    @GetMapping("/sent/{email}")
    public ResponseEntity<?> sentItems(@PathVariable String email) throws Exception {
        try {
            List<String> sentEmails = gmailService.sentEmailList(email);

            return ResponseEntity.ok(sentEmails);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/sent/{email}/{messageId}")
    public ResponseEntity<?> readSentEmailBody(
            @PathVariable String email,
            @PathVariable String messageId,
            @PathVariable(required = false) boolean isDraft
    ) {
        try {
            Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);

            return ResponseEntity.ok(emailBody);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read sent email: " + e.getMessage());
        }
    }


    @GetMapping("/draft/{email}")
    public ResponseEntity<?> listOfDraftEmail(@PathVariable String email) {
        try {
            List<String> strings = gmailService.draftEmailsList(email);
            return ResponseEntity.ok(strings);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read draft email: " + e.getMessage());
        }
    }

    @GetMapping("/draft/{email}/{messageId}")
    public ResponseEntity<?> readDraftEmailBody(
            @PathVariable String email,
            @PathVariable String messageId,
            @PathVariable(required = true) boolean isDraft
    ) {
        try {
            Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);

            return ResponseEntity.ok(emailBody);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read sent email: " + e.getMessage());
        }
    }

    @GetMapping("/spam/{email}")
    public ResponseEntity<?> listOfSpam(@PathVariable String email) {
        try {
            List<String> draftEmailsList = gmailService.draftEmailsList(email);

            return ResponseEntity.ok(draftEmailsList);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read sent email: " + ex.getMessage());
        }
    }

    @GetMapping("/spam/{userEmail}/{messageId}")
    public ResponseEntity<?> readSpamEmail(
            @PathVariable String email,
            @PathVariable String messageId,
            @PathVariable(required = false) boolean isDraft
    ) {
        try {
            Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);
            return ResponseEntity.ok(emailBody);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read sent email: " + e.getMessage());
        }
    }

    @GetMapping("/moveToTrash/{email}/{messageId}")
    public ResponseEntity<?> moveToTrashEmail(@PathVariable String email,
                                              @PathVariable String messageId) {
        try {
            gmailService.moveEmailToTrash(email, messageId);
            return ResponseEntity.ok("Email moved to Trash successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to move email in trash: " + e.getMessage());
        }
    }

    @GetMapping("/unTrashEmail/{email}/{messageId}")
    public ResponseEntity<?> unTrashEmail(@PathVariable String email,
                                              @PathVariable String messageId) {
        try {
            gmailService.unTrashEmail(email,messageId);
            return ResponseEntity.ok("Email moved from Trash successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to move email from trash: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{email}/{messageId}")
    public ResponseEntity<?> delete(@PathVariable String email,
                                    @PathVariable String messageId) throws Exception {
        try {
            gmailService.deleteEmail(email, messageId);
            return ResponseEntity.ok("Email deleted successfully!");
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
