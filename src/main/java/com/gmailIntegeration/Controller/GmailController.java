package com.gmailIntegeration.Controller;


import com.gmailIntegeration.Service.GmailService;
import com.gmailIntegeration.exceptions.UnauthorizedUserException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
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

    //get current profile
    @GetMapping("/currentUser/{email}")
    public ResponseEntity<?> getCurrentUser(@PathVariable String email) throws UnauthorizedUserException {
        try {
            Map<String, Object> currentUserProfile = gmailService.currentUserProfile(email);
            
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", currentUserProfile
            ));
        }catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "status", HttpStatus.UNAUTHORIZED,
                        "message", "User not authorized. Please authorize Gmail access.",
                        "authorizationUrl", e.getAuthorizationUrl()
                ));

        }catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR,
                    "message", e.getMessage()
            ));
        }
    }

    // list of labels
    @GetMapping("/labels/{email}")
    public ResponseEntity<?> getLabels(@PathVariable String email) {
        try {
            Map<String, String> labelList = gmailService.labelList(email);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", labelList
            ));
        }catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access." + e.getMessage(),
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR,
                    "message", e.getMessage()
            ));
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
            List<Map<String, Object>> emailsByLabel = gmailService.getEmailsByLabel(email, label);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", emailsByLabel
            ));
        }catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }


    //inbox mail list
    @GetMapping("/inbox/{email}")
    public ResponseEntity<?> inbox(@PathVariable String email) throws Exception {
        try {
            List<Map<String, Object>> inboxEmailList = gmailService.getInboxEmailList(email);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", inboxEmailList
            ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //read full mail from inbox
    @GetMapping("/inbox/{email}/{messageId}")
    public ResponseEntity<?> readEmailBody(
            @PathVariable String email,
            @PathVariable String messageId,
            @RequestParam(required = false) boolean isDraft //if the mail is draft then give true
    ) {
        try {
//            Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);
            Map<String, Object> emailBody1 = gmailService.readFullEmailBody(email, messageId, isDraft);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", emailBody1
            ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message","Failed to read message" + e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }


    //for making start currently we are apply on Inbox
    //for each category also we can do
    @PostMapping("/inbox/{email}/{messageId}")
    public ResponseEntity<?> toggleStarredEmail(
            @PathVariable String email,
            @PathVariable String messageId,
            @RequestParam boolean starStatus
            //in future if can make category filed dynamic
    ) {
        try {

            gmailService.toggleStar(email, messageId, starStatus);
            return ResponseEntity
                    .status(HttpStatus.OK).body(Map.of(
                            "status", HttpStatus.OK,
                            "data", "Email " + (starStatus ? "starred" : "un-starred") + " successfully!"
                    ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //list of stared mails from inbox
    @GetMapping("inbox/{email}/starred")
    public ResponseEntity<?> listOfStarredEmail(@PathVariable String email) {
        try {
            List<Map<String, Object>> starredEmailList = gmailService.getStarredEmailList(email);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", starredEmailList
            ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }


    //send mail to single user for sending mail to multi user apply loop in service logic
    @PostMapping("/send/{email}")
    public ResponseEntity<?> send(@PathVariable String email,
                       @RequestParam String To,
                       @RequestParam(required = false) String CC,
                       @RequestParam(required = false) String BCC,
                       @RequestParam String subject,
                       @RequestParam String body) throws Exception {
        try {
            gmailService.sendEmailWithoutAttachment(email, To, BCC, CC, subject, body);
            String data = "Email sent successfully! + to: " + To + " from: " + email;
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", data
            ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //sending mail with attachment
    //note:- for multiple sending mail use loop in service logic
    @PostMapping("/send-email/{userEmail}")
    public ResponseEntity<?> sendEmailWithAttachment(
            @PathVariable String userEmail,
            @RequestParam List<String> To,
            @RequestParam(required = false) List<String> Cc,
            @RequestParam(required = false) List<String> Bcc,
            @RequestParam String subject,
            @RequestParam String bodyText,
            @RequestParam(required = false) List<MultipartFile> attachmentFiles) {

        try {
            for (MultipartFile file : attachmentFiles) {
                System.out.println("File Name: " + file.getOriginalFilename());
                System.out.println("----------------------");
            }
            gmailService.sendEmailWithAttachment(userEmail, To, Cc ,Bcc, subject, bodyText, attachmentFiles);

            String data = "Email sent successfully! + to: " + To + "from: " + userEmail;
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", data
            ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }


    //list of sent mails
    @GetMapping("/sent/{email}")
    public ResponseEntity<?> sentEmailList(@PathVariable String email) throws Exception {
        try {
            List<Map<String, Object>> sentEmailList = gmailService.sentEmailList(email);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", sentEmailList
            ));

        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //read sent mail with body
    @GetMapping("/sent/{email}/{messageId}")
    public ResponseEntity<?> readSentEmailBody(
            @PathVariable String email,
            @PathVariable String messageId,
            @RequestParam(required = false) boolean isDraft
    ) {
        try {
//            Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);
            Map<String, Object> emailBody1 = gmailService.readFullEmailBody(email, messageId, isDraft);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status",HttpStatus.OK,
                    "data", emailBody1
            ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }


    //list of draft mails
    @GetMapping("/draft/{email}")
    public ResponseEntity<?> listOfDraftEmail(@PathVariable String email) {
        try {
            List<Map<String, Object>> draftEmailList = gmailService.draftEmailList(email);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status",HttpStatus.OK,
                    "data", draftEmailList
            ));
        }catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //read draft mail
    @GetMapping("/draft/{email}/{messageId}")
    public ResponseEntity<?> readDraftEmailBody(
            @PathVariable String email,
            @PathVariable String messageId,
            @RequestParam(required = true) boolean isDraft
    ) {
        try {
            //Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);
            Map<String, Object> emailBody = gmailService.readFullEmailBody(email, messageId, isDraft);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", emailBody
            ));

        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //list of spam mails
    @GetMapping("/spam/{email}")
    public ResponseEntity<?> listOfSpam(@PathVariable String email) {
        try {
            List<Map<String, Object>> listOfSpamEmail = gmailService.spamEmailList(email);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", listOfSpamEmail
            ));

        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //read spam mail
    @GetMapping("/spam/{email}/{messageId}")
    public ResponseEntity<?> readSpamEmail(
            @PathVariable String email,
            @PathVariable String messageId,
            @PathVariable(required = false) boolean isDraft
    ) {
        try {
            //Map<String, Object> emailBody = gmailService.readAnyEmailBody(email, messageId, isDraft);

            Map<String, Object> emailBody = gmailService.readFullEmailBody(email, messageId, isDraft);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status", HttpStatus.OK,
                    "data", emailBody
            ));

        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //moved mail to trash
    @GetMapping("/moveToTrash/{email}/{messageId}")
    public ResponseEntity<?> moveToTrashEmail(@PathVariable String email,
                                              @PathVariable String messageId) {
        try {
            String moveEmailToTrash = gmailService.moveEmailToTrash(email, messageId);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "status",HttpStatus.OK,
                    "message", moveEmailToTrash
            ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    //move mail from trash back
    @GetMapping("/unTrash/{email}/{messageId}")
    public ResponseEntity<?> unTrashEmail(@PathVariable String email,
                                          @PathVariable String messageId) {
        try {
            String unTrashEmail = gmailService.unTrashEmail(email, messageId);
            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of(
                            "status", HttpStatus.OK,
                            "message", unTrashEmail
                    )
            );
        }catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @DeleteMapping("/delete/{email}/{messageId}")
    public ResponseEntity<?> delete(@PathVariable String email,
                                    @PathVariable String messageId) throws Exception {
        try {
            String deletedEmail = gmailService.deleteEmail(email, messageId);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "message", deletedEmail,
                    "status", HttpStatus.OK
            ));
        } catch (UnauthorizedUserException e) {
            System.out.println(e.getMessage());
            //when user is not authorized, generate OAuth url
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED,
                    "message", "User not authorized. Please authorize Gmail access.",
                    "authorizationUrl", e.getAuthorizationUrl()
            ));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "status", HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }


    @GetMapping("/{email}/{messageId}/{attachmentId}/view")
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
            String extension = "bin";
            if (detectedMimeType.contains("/")) {
                extension = detectedMimeType.split("/")[1];
            }

            //getting the original name for file
            Message message = gmail.users().messages().get("me", messageId).execute();
            String filename = message.getPayload().getFilename() + extension;

            //Return the correct content type and inline display
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(detectedMimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"") //for download use "attachment; filename=..."
                    .body(new ByteArrayResource(fileBytes));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching attachment: " + e.getMessage());
        }
    }


    @GetMapping("/{email}/{messageId}/{attachmentId}/download")
    public ResponseEntity<?> downloadAttachment(
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
            String extension = "bin";
            if (detectedMimeType.contains("/")) {
                extension = detectedMimeType.split("/")[1];
            }

            //getting the original name for file
            Message message = gmail.users().messages().get("me", messageId).execute();
            String filename = message.getPayload().getFilename() + extension;

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

}


