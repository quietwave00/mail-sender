package com.example.mailsender.dto.response;

import com.example.mailsender.dto.MailPreview;
import lombok.Getter;

import java.util.List;

@Getter
public class MailPreviewListResponse {

    private List<MailPreview> previews;
    private int totalCount;
    private String templateSubject;
    private String templateBody;
    private byte[] templateFile;
    private String templateFileName;
    private String snapshotId;

    public MailPreviewListResponse(
            List<MailPreview> previews,
            int totalCount,
            String templateSubject,
            String templateBody,
            byte[] templateFile,
            String templateFileName
    ) {
        this(previews, totalCount, templateSubject, templateBody, templateFile, templateFileName, null);
    }

    public MailPreviewListResponse(
            List<MailPreview> previews,
            int totalCount,
            String templateSubject,
            String templateBody,
            byte[] templateFile,
            String templateFileName,
            String snapshotId
    ) {
        this.previews = previews;
        this.totalCount = totalCount;
        this.templateSubject = templateSubject;
        this.templateBody = templateBody;
        this.templateFile = templateFile;
        this.templateFileName = templateFileName;
        this.snapshotId = snapshotId;
    }

}
