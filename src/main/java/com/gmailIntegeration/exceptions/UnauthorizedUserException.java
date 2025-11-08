package com.gmailIntegeration.exceptions;

import com.gmailIntegeration.Configuration.GmailConfig1;
import org.springframework.beans.factory.annotation.Autowired;


public class UnauthorizedUserException extends RuntimeException {

    private final String authorizationUrl;

    public UnauthorizedUserException(String message, String authorizationUrl) {
        super(message);
        this.authorizationUrl = authorizationUrl;
    }

    public String getAuthorizationUrl() {
        System.out.println(authorizationUrl);
        return authorizationUrl;
    }


}
