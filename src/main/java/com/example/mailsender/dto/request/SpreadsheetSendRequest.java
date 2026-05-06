package com.example.mailsender.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SpreadsheetSendRequest {
    private String snapshotId;
    private String spreadsheetUrl;
    private String sheetName;
    private Integer sheetGid;
    private SpreadsheetColumnMappingRequest columnMapping;
    private List<Integer> selectedRowIds;
}
