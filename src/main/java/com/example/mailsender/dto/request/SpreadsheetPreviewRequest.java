package com.example.mailsender.dto.request;

import lombok.Data;

@Data
public class SpreadsheetPreviewRequest {
    private String spreadsheetUrl;
    private String sheetName;
    private Integer sheetGid;
    private SpreadsheetColumnMappingRequest columnMapping;
}
