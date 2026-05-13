package com.example.mailsender.service.template;

import com.example.mailsender.dto.request.SpreadsheetColumnMappingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ColumnMappingService {
    private static final String COLUMN_MAPPING_FILE_SUFFIX = "-column-mapping.json";

    private final ObjectMapper objectMapper;
    private final UserScopedFileStorageService userScopedFileStorageService;

    public SpreadsheetColumnMappingRequest getColumnMapping() {
        Path storageFile = resolveStorageFile();
        if (!Files.exists(storageFile)) {
            return null;
        }

        try {
            SpreadsheetColumnMappingRequest columnMapping =
                    objectMapper.readValue(storageFile.toFile(), SpreadsheetColumnMappingRequest.class);
            validateColumnMapping(columnMapping);
            return columnMapping;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load column mapping", e);
        }
    }

    public void saveColumnMapping(SpreadsheetColumnMappingRequest columnMapping) {
        validateColumnMapping(columnMapping);

        try {
            objectMapper.writeValue(resolveStorageFile().toFile(), columnMapping);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save column mapping", e);
        }
    }

    private Path resolveStorageFile() {
        return userScopedFileStorageService.resolveCurrentUserFile(COLUMN_MAPPING_FILE_SUFFIX);
    }

    private void validateColumnMapping(SpreadsheetColumnMappingRequest columnMapping) {
        if (columnMapping == null) {
            throw new IllegalArgumentException("열 매핑 정보가 없습니다.");
        }

        if (columnMapping.getNameColumn() < 0
                || columnMapping.getEmailColumn() < 0
                || columnMapping.getTicketColumn() < 0) {
            throw new IllegalArgumentException("열 매핑 값이 올바르지 않습니다.");
        }
    }
}
