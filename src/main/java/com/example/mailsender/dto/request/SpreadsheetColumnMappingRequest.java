package com.example.mailsender.dto.request;

import lombok.Data;

@Data
public class SpreadsheetColumnMappingRequest {
    private int nameColumn;
    private int emailColumn;
    private int ticketColumn;
}
