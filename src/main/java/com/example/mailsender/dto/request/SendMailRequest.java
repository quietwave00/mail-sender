package com.example.mailsender.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SendMailRequest {

    private MultipartFile file;
    private int nameColumn;
    private int emailColumn;
    private int ticketColumn;

}
