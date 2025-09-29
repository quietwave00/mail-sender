package com.example.mailsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MailSenderApplication {

    public static void main(String[] args) {
        System.out.println("Hello, Mail-Sender");
        SpringApplication.run(MailSenderApplication.class, args);
    }

}
