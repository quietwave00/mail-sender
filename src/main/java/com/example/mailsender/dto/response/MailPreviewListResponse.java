package com.example.mailsender.dto.response;

import com.example.mailsender.dto.MailPreview;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MailPreviewListResponse {

    private List<MailPreview> previews;
    private int totalCount;
    private String templateSubject;
    private String templateBody;
    private byte[] templateFile;
    private String templateFileName;

}
