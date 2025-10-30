package com.gmailIntegeration.Controller;

import com.gmailIntegeration.Service.GmailService;
import com.google.api.services.gmail.model.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    @Autowired
    private GmailService gmailService;

    /**
     * GET endpoint to list all Gmail labels
     * URL: http://localhost:8080/api/gmail/labels
     */
    @GetMapping("/labels")
    public ResponseEntity<?> getLabels() {
        try {
            List<String> labels = gmailService.listLabels();
            return ResponseEntity.ok(labels);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching labels: " + e.getMessage());
        }
    }

    /**
     * GET endpoint to get a specific label by ID
     * URL: http://localhost:8080/api/gmail/labels/{labelId}
     */
    @GetMapping("/labels/{labelId}")
    public ResponseEntity<?> getLabel(@PathVariable String labelId) {
        try {
            Label label = gmailService.getLabel(labelId);
            return ResponseEntity.ok(label);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching label: " + e.getMessage());
        }
    }

    /**
     * GET endpoint to get the count of labels
     * URL: http://localhost:8080/api/gmail/labels/count
     */
    @GetMapping("/labels/count")
    public ResponseEntity<?> getLabelCount() {
        try {
            int count = gmailService.getLabelCount();
            return ResponseEntity.ok("Total labels: " + count);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error counting labels: " + e.getMessage());
        }
    }

    @GetMapping("/currentUser")
    public ResponseEntity<String> getCurrentUser() throws IOException {
        String currentUserEmail = gmailService.getCurrentUserEmail();
        return ResponseEntity.ok("Logged in as: " + currentUserEmail);
    }

    @PostMapping("/sendEmail")
    public ResponseEntity<?> sendEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body) {
        try {
            gmailService.sendEmail(to, subject, body);
            return ResponseEntity.status(HttpStatus.OK).body("Email sent successfully to: " + to + "From logged-in user." + gmailService.getCurrentUserEmail());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email: " + e.getMessage());
        }
    }

    @GetMapping("/inbox")
    public List<String> getInbox() {
        try {
            return gmailService.getInboxEmails();
        } catch (Exception e) {
            return List.of("❌ Error fetching inbox: " + e.getMessage());
        }
    }

}
