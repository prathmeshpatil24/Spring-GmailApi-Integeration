package com.gmailIntegeration;

import com.gmailIntegeration.Service.GmailService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class GmailIntegerationApplication {

	public static void main(String[] args) throws Exception {
		ApplicationContext context =SpringApplication.run(GmailIntegerationApplication.class, args);
        System.out.println("project is working..!");

        GmailService gmailService = context.getBean(GmailService.class);

       // long totalMailCount = gmailService.getTotalMailCount("patilprathmesh365@gmail.com");

        //System.out.println("total mails:- " + totalMailCount);


    }

}
