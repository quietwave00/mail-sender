package com.example.mailsender.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MailPreview {

    private String email;
    private String name;
    private String ticketNumbers;
    private String ticketCount;

}
