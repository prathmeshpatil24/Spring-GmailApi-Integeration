package com.gmailIntegeration.Controller;


import com.gmailIntegeration.Service.GmailService;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.MessagePartBody;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.Base64;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailService gmailService;
//    private final JsonOrTextConversion jsonOrTextConversion;

    public GmailController(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @GetMapping("/currentUser/{email}")
    public ResponseEntity<String> getCurrentUser(@PathVariable String email) {
        try {
            List<String> currentUserProfile = gmailService.currentUserProfile(email);
            return ResponseEntity.ok("Logged in as: " + currentUserProfile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //$
    @GetMapping("/labels/{email}")
    public ResponseEntity<?> getLabels(@PathVariable String email) {
        try {
            List<String> labels = gmailService.listLabels(email);
            return ResponseEntity.ok(labels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //$
    //dynamic label fetching
    @GetMapping("/labels/{email}/{label}")
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
        try {
            List<Map<String, Object>> inboxEmails = gmailService.getInboxEmails(email);
            return ResponseEntity.ok(inboxEmails);
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
//            Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);

            Map<String, Object> emailBody1 = gmailService.readFullEmailBody(email, messageId, isDraft);

            return ResponseEntity.ok(emailBody1);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read email: " + e.getMessage());
        }
    }


    @PostMapping("/inbox/{email}/{messageId}/star")
    public ResponseEntity<?> toggleStarredEmail(
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
    public ResponseEntity<?> listOfStarredEmail(@PathVariable String email) {
        try {
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
            @RequestParam(required = false) List<MultipartFile> attachmentFiles) {

        try {
            for (MultipartFile file : attachmentFiles) {
                System.out.println("File Name: " + file.getOriginalFilename());
            }
            gmailService.sendEmailWithAttachment(userEmail, toEmail, subject, bodyText, attachmentFiles);
            return ResponseEntity.ok("Email sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send email: " + e.getMessage());
        }
    }


    @GetMapping("/sent/{email}")
    public ResponseEntity<?> sentItems(@PathVariable String email) throws Exception {
        try {
            List<Map<String, Object>> sentEmailList = gmailService.sentEmailList(email);

            return ResponseEntity.ok(sentEmailList);
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
//            Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);
            Map<String, Object> emailBody1 = gmailService.readFullEmailBody(email, messageId, isDraft);
            return ResponseEntity.ok(emailBody1);
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
            gmailService.unTrashEmail(email, messageId);
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


    @GetMapping("/{email}/{messageId}/{attachmentId}")
    public ResponseEntity<?> viewAttachment(
            @PathVariable String email,
            @PathVariable String messageId,
            @PathVariable String attachmentId) {
        try {

            Gmail gmail = gmailService.getGmail(email);

            // Fetch the attachment
            MessagePartBody attachPart = gmail.users()
                    .messages()
                    .attachments()
                    .get(email, messageId, attachmentId)
                    .execute();

            // Decode base64 data
            byte[] fileBytes = Base64.getUrlDecoder().decode(attachPart.getData());

            //Try to detect the real MIME type
            String detectedMimeType = null;
            try {
                detectedMimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(fileBytes));
            } catch (Exception ignored) {}

            // Fallback to default
            if(detectedMimeType == null) {
                detectedMimeType = "application/octet-stream";
            }

            //Extract extension from MIME
            String extension = detectedMimeType.split("/")[1];
            String filename = "attachment." + extension;

            //Return the correct content type and inline display
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(detectedMimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"") //for download use "attachment; filename=..."
                    .body(new ByteArrayResource(fileBytes));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching attachment: " + e.getMessage());
        }
    }


    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return "bin"; // default unknown
        }

        switch (mimeType) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "application/pdf":
                return "pdf";
            case "text/plain":
                return "txt";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            case "application/vnd.ms-excel":
                return "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return "xlsx";
            default:
                return "bin";
        }
    }


    //1
//    @GetMapping("/attachments/{email}/{messageId}/{attachmentId}")
//    public ResponseEntity<?> viewAttachment(@PathVariable String email,
//                                            @PathVariable String messageId,
//                                            @PathVariable String attachmentId) {
//        try {
//
//            Gmail gmail = gmailService.getGmail(email);
//
//            Message message = gmail.users().messages().get("me", messageId).execute();
//            MessagePart messagePayload = message.getPayload();
//            String mimeType = messagePayload.getMimeType();
//
//
//            MessagePartBody attachPart = gmail.users()
//                    .messages()
//                    .attachments()
//                    .get(email, messageId, attachmentId)
//                    .execute();
//
//            // Decode the Base64 data
//            byte[] fileBytes = Base64.getUrlDecoder().decode(attachPart.getData());
//
//            // Optionally detect content type (you can also store it from original part)
//
//       //String contentType = Files.probeContentType(Paths.get("dummy." + getExtensionFromMimeType(mimeType)));
//
//            return ResponseEntity.ok()
//                    .contentType(MediaType.parseMediaType(
//                            mimeType != null ? mimeType : "application/octet-stream"))
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"attachment\"")
//                    .body(new ByteArrayResource(fileBytes));
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error fetching attachment: " + e.getMessage());
//        }
//    }

}


