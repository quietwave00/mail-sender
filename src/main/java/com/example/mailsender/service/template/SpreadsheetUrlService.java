package com.example.mailsender.service.template;

import com.example.mailsender.dto.request.SpreadsheetUrlRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class SpreadsheetUrlService {
    private static final String SPREADSHEET_URL_FILE_SUFFIX = "-spreadsheet-url.json";

    private final ObjectMapper objectMapper;
    private final UserScopedFileStorageService userScopedFileStorageService;

    public SpreadsheetUrlRequest getSpreadsheetUrl() {
        Path storageFile = resolveStorageFile();
        if (!Files.exists(storageFile)) {
            return null;
        }

        try {
            SpreadsheetUrlRequest spreadsheetUrl =
                    objectMapper.readValue(storageFile.toFile(), SpreadsheetUrlRequest.class);
            validateSpreadsheetUrlPayload(spreadsheetUrl);
            return spreadsheetUrl;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load spreadsheet url", e);
        }
    }

    public void saveSpreadsheetUrl(SpreadsheetUrlRequest request) {
        validateSpreadsheetUrlPayload(request);

        Path storageFile = resolveStorageFile();
        String spreadsheetUrl = request.getSpreadsheetUrl().trim();

        try {
            if (spreadsheetUrl.isEmpty()) {
                Files.deleteIfExists(storageFile);
                return;
            }

            SpreadsheetUrlRequest payload = new SpreadsheetUrlRequest();
            payload.setSpreadsheetUrl(spreadsheetUrl);
            objectMapper.writeValue(storageFile.toFile(), payload);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save spreadsheet url", e);
        }
    }

    private Path resolveStorageFile() {
        return userScopedFileStorageService.resolveCurrentUserFile(SPREADSHEET_URL_FILE_SUFFIX);
    }

    private void validateSpreadsheetUrlPayload(SpreadsheetUrlRequest request) {
        if (request == null || request.getSpreadsheetUrl() == null) {
            throw new IllegalArgumentException("스프레드시트 URL 정보가 없습니다.");
        }
    }
}
