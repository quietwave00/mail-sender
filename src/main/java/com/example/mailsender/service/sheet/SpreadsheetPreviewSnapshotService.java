package com.example.mailsender.service.sheet;

import com.example.mailsender.dto.TicketInfo;
import com.example.mailsender.dto.request.SpreadsheetColumnMappingRequest;
import com.example.mailsender.dto.request.SpreadsheetPreviewRequest;
import com.example.mailsender.exception.CustomException;
import com.example.mailsender.exception.ExceptionCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SpreadsheetPreviewSnapshotService {

    private final ConcurrentMap<String, SpreadsheetPreviewSnapshot> snapshots = new ConcurrentHashMap<>();

    public String createSnapshot(SpreadsheetPreviewRequest request, List<TicketInfo> tickets) {
        String snapshotId = UUID.randomUUID().toString();
        SpreadsheetPreviewSnapshot snapshot = new SpreadsheetPreviewSnapshot(
                snapshotId,
                request.getSpreadsheetUrl(),
                request.getSheetName(),
                request.getSheetGid(),
                copyColumnMapping(request.getColumnMapping()),
                List.copyOf(tickets),
                Instant.now()
        );
        snapshots.put(snapshotId, snapshot);
        return snapshotId;
    }

    public SpreadsheetPreviewSnapshot getSnapshot(String snapshotId) {
        SpreadsheetPreviewSnapshot snapshot = snapshots.get(snapshotId);
        if (snapshot == null) {
            throw new CustomException(ExceptionCode.INVALID_SPREADSHEET_SNAPSHOT);
        }
        return snapshot;
    }

    public void removeSnapshot(String snapshotId) {
        if (snapshotId == null || snapshotId.isBlank()) {
            return;
        }
        snapshots.remove(snapshotId);
    }

    private SpreadsheetColumnMappingRequest copyColumnMapping(SpreadsheetColumnMappingRequest source) {
        if (source == null) {
            return null;
        }

        SpreadsheetColumnMappingRequest copy = new SpreadsheetColumnMappingRequest();
        copy.setNameColumn(source.getNameColumn());
        copy.setEmailColumn(source.getEmailColumn());
        copy.setTicketColumn(source.getTicketColumn());
        return copy;
    }

    public record SpreadsheetPreviewSnapshot(
            String snapshotId,
            String spreadsheetUrl,
            String sheetName,
            Integer sheetGid,
            SpreadsheetColumnMappingRequest columnMapping,
            List<TicketInfo> tickets,
            Instant createdAt
    ) {
        public SpreadsheetPreviewSnapshot {
            tickets = List.copyOf(new ArrayList<>(tickets));
        }
    }
}
