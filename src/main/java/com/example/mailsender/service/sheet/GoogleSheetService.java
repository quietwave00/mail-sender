package com.example.mailsender.service.sheet;

import com.example.mailsender.dto.TicketInfo;
import com.example.mailsender.dto.request.SpreadsheetColumnMappingRequest;
import com.example.mailsender.dto.request.SpreadsheetPreviewRequest;
import com.example.mailsender.dto.request.SpreadsheetSendRequest;
import com.example.mailsender.exception.CustomException;
import com.example.mailsender.exception.ExceptionCode;
import com.example.mailsender.service.auth.GoogleAuthorizedClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleSheetService {
    private static final String SHEETS_API_BASE_URL = "https://sheets.googleapis.com/v4/spreadsheets";
    private static final int HEADER_ROW_INDEX = 0;
    private static final Pattern SPREADSHEET_ID_PATTERN =
            Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
    private static final Pattern GID_PATTERN =
            Pattern.compile("[?&]gid=(\\d+)");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern TICKET_NUMBER_PATTERN =
            Pattern.compile("[0-9+]+");

    private final GoogleAuthorizedClientService googleAuthorizedClientService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SpreadsheetSource parseSource(String spreadsheetUrl, String sheetName, Integer sheetGid) {
        if (spreadsheetUrl == null || spreadsheetUrl.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_SPREADSHEET_URL);
        }

        String spreadsheetId = extractSpreadsheetId(spreadsheetUrl);
        Integer resolvedGid = sheetGid != null ? sheetGid : extractSheetGid(spreadsheetUrl);
        return new SpreadsheetSource(spreadsheetId, sheetName, resolvedGid);
    }

    public SpreadsheetContent readSpreadsheet(SpreadsheetPreviewRequest request) {
        SpreadsheetSource source = parseSource(
                request.getSpreadsheetUrl(),
                request.getSheetName(),
                request.getSheetGid()
        );
        return readSpreadsheet(source);
    }

    public SpreadsheetContent readSpreadsheet(SpreadsheetSendRequest request) {
        SpreadsheetSource source = parseSource(
                request.getSpreadsheetUrl(),
                request.getSheetName(),
                request.getSheetGid()
        );
        return readSpreadsheet(source);
    }

    public SpreadsheetContent readSpreadsheet(SpreadsheetSource source) {
        String accessToken = getAccessToken();
        JsonNode spreadsheetJson = fetchSpreadsheetMetadata(source.spreadsheetId(), accessToken);
        String spreadsheetTitle = spreadsheetJson.path("properties").path("title").asText(null);
        SheetSelection sheetSelection = selectSheet(spreadsheetJson.path("sheets"), source);
        java.util.List<java.util.List<String>> values = fetchSheetValues(source.spreadsheetId(), sheetSelection.title(), accessToken);

        return new SpreadsheetContent(
                source.spreadsheetId(),
                spreadsheetTitle,
                sheetSelection.title(),
                sheetSelection.sheetGid(),
                values
        );
    }

    public List<TicketInfo> loadPreviewTickets(SpreadsheetPreviewRequest request) {
        SpreadsheetContent content = readSpreadsheet(request);
        return toTicketInfos(content.rows(), request.getColumnMapping(), null);
    }

    public List<TicketInfo> loadSendTickets(SpreadsheetSendRequest request) {
        SpreadsheetContent content = readSpreadsheet(request);
        Set<Integer> selectedRowIds = request.getSelectedRowIds() == null
                ? null
                : new HashSet<>(request.getSelectedRowIds());
        return toTicketInfos(content.rows(), request.getColumnMapping(), selectedRowIds);
    }

    private String extractSpreadsheetId(String spreadsheetUrl) {
        Matcher matcher = SPREADSHEET_ID_PATTERN.matcher(spreadsheetUrl);
        if (!matcher.find()) {
            throw new CustomException(ExceptionCode.INVALID_SPREADSHEET_URL);
        }
        return matcher.group(1);
    }

    private Integer extractSheetGid(String spreadsheetUrl) {
        Matcher matcher = GID_PATTERN.matcher(spreadsheetUrl);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String getAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new CustomException(ExceptionCode.SPREADSHEET_READ_FAILED);
        }

        return googleAuthorizedClientService.getAuthorizedClient(authentication)
                .getAccessToken()
                .getTokenValue();
    }

    private JsonNode fetchSpreadsheetMetadata(String spreadsheetId, String accessToken) {
        String requestUrl = SHEETS_API_BASE_URL + "/" + spreadsheetId
                + "?fields=properties.title,sheets.properties(sheetId,title,sheetType)";
        return executeGet(requestUrl, accessToken);
    }

    private java.util.List<java.util.List<String>> fetchSheetValues(
            String spreadsheetId,
            String sheetTitle,
            String accessToken
    ) {
        String encodedRange = URLEncoder.encode(toSheetRange(sheetTitle), StandardCharsets.UTF_8);
        String requestUrl = SHEETS_API_BASE_URL + "/" + spreadsheetId + "/values/" + encodedRange
                + "?majorDimension=ROWS";

        JsonNode valuesJson = executeGet(requestUrl, accessToken);
        java.util.List<java.util.List<String>> rows = new ArrayList<>();

        JsonNode valuesNode = valuesJson.path("values");
        if (!valuesNode.isArray()) {
            return rows;
        }

        for (JsonNode rowNode : valuesNode) {
            java.util.List<String> row = new ArrayList<>();
            for (JsonNode cellNode : rowNode) {
                row.add(cellNode.asText(""));
            }
            rows.add(row);
        }

        return rows;
    }

    private JsonNode executeGet(String requestUrl, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw createSpreadsheetReadException(requestUrl, response);
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new CustomException(ExceptionCode.SPREADSHEET_READ_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ExceptionCode.SPREADSHEET_READ_FAILED);
        }
    }

    private CustomException createSpreadsheetReadException(String requestUrl, HttpResponse<String> response) {
        String responseBody = response.body();
        String googleMessage = extractGoogleErrorMessage(responseBody);

        log.error(
                "Google Sheets API request failed. status={}, url={}, body={}",
                response.statusCode(),
                requestUrl,
                responseBody
        );

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            String message = "스프레드시트 접근 권한이 없거나 추가 동의가 필요합니다.";
            if (googleMessage != null && !googleMessage.isBlank()) {
                message += " Google 응답: " + googleMessage;
            }
            if (googleMessage != null && googleMessage.toLowerCase().contains("insufficient authentication scopes")) {
                message += " 로그아웃 후 다시 로그인해서 스프레드시트 읽기 권한에 다시 동의해 주세요.";
            }
            return new CustomException(ExceptionCode.SPREADSHEET_ACCESS_DENIED, message);
        }

        String message = ExceptionCode.SPREADSHEET_READ_FAILED.getMessage();
        if (googleMessage != null && !googleMessage.isBlank()) {
            message += " Google 응답: " + googleMessage;
        }
        return new CustomException(ExceptionCode.SPREADSHEET_READ_FAILED, message);
    }

    private String extractGoogleErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            JsonNode errorNode = objectMapper.readTree(responseBody).path("error");
            if (errorNode.isMissingNode()) {
                return null;
            }

            String message = errorNode.path("message").asText(null);
            if (message != null && !message.isBlank()) {
                return message;
            }

            JsonNode errorsNode = errorNode.path("errors");
            if (errorsNode.isArray() && !errorsNode.isEmpty()) {
                return errorsNode.get(0).path("message").asText(null);
            }
            return null;
        } catch (IOException e) {
            return responseBody;
        }
    }

    private SheetSelection selectSheet(JsonNode sheetsNode, SpreadsheetSource source) {
        if (!sheetsNode.isArray() || sheetsNode.isEmpty()) {
            throw new CustomException(ExceptionCode.SPREADSHEET_SHEET_NOT_FOUND);
        }

        SheetSelection firstGridSheet = null;
        for (JsonNode sheetNode : sheetsNode) {
            JsonNode propertiesNode = sheetNode.path("properties");
            if (!"GRID".equals(propertiesNode.path("sheetType").asText("GRID"))) {
                continue;
            }

            int sheetId = propertiesNode.path("sheetId").asInt(-1);
            String title = propertiesNode.path("title").asText(null);
            if (firstGridSheet == null) {
                firstGridSheet = new SheetSelection(title, sheetId);
            }
            if (matchesRequestedSheet(source, title, sheetId)) {
                return new SheetSelection(title, sheetId);
            }
        }

        if (source.sheetName() == null && source.sheetGid() == null) {
            if (firstGridSheet != null) {
                return firstGridSheet;
            }
        }

        throw new CustomException(ExceptionCode.SPREADSHEET_SHEET_NOT_FOUND);
    }

    private boolean matchesRequestedSheet(SpreadsheetSource source, String title, int sheetId) {
        if (source.sheetName() != null && !source.sheetName().isBlank()) {
            return source.sheetName().equals(title);
        }

        if (source.sheetGid() != null) {
            return source.sheetGid() == sheetId;
        }

        return false;
    }

    private String toSheetRange(String sheetTitle) {
        String escapedSheetTitle = sheetTitle.replace("'", "''");
        return "'" + escapedSheetTitle + "'";
    }

    private List<TicketInfo> toTicketInfos(
            List<List<String>> rows,
            SpreadsheetColumnMappingRequest columnMapping,
            Set<Integer> selectedRowIds
    ) {
        validateColumnMapping(columnMapping);

        List<TicketInfo> tickets = new ArrayList<>();
        for (int rowIndex = HEADER_ROW_INDEX + 1; rowIndex < rows.size(); rowIndex++) {
            if (isRowEmpty(rows.get(rowIndex))) {
                break;
            }

            int rowNumber = rowIndex + 1;
            if (selectedRowIds != null && !selectedRowIds.contains(rowNumber)) {
                continue;
            }

            TicketInfo ticketInfo = toTicketInfo(rowNumber, rows.get(rowIndex), columnMapping);
            if (ticketInfo != null) {
                tickets.add(ticketInfo);
            }
        }
        return tickets;
    }

    private TicketInfo toTicketInfo(
            int rowNumber,
            List<String> row,
            SpreadsheetColumnMappingRequest columnMapping
    ) {
        if (isRowEmpty(row)) {
            return null;
        }

        String email = getCellValue(row, columnMapping.getEmailColumn());
        String name = getCellValue(row, columnMapping.getNameColumn());
        String ticketNumberCell = getCellValue(row, columnMapping.getTicketColumn());

        String[] ticketNumbers = ticketNumberCell.isEmpty()
                ? new String[0]
                : ticketNumberCell.split("\\+");
        validateData(email, ticketNumbers);

        List<Integer> ticketNumberList = new ArrayList<>();
        for (String ticketNumber : ticketNumbers) {
            String trimmedTicketNumber = ticketNumber.trim();
            if (trimmedTicketNumber.isEmpty()) {
                continue;
            }
            ticketNumberList.add(Integer.parseInt(trimmedTicketNumber));
        }

        return new TicketInfo(rowNumber, email, name, ticketNumberList);
    }

    private void validateColumnMapping(SpreadsheetColumnMappingRequest columnMapping) {
        if (columnMapping == null) {
            throw new CustomException(ExceptionCode.INVALID_SPREADSHEET_COLUMN_MAPPING);
        }

        if (columnMapping.getNameColumn() < 0
                || columnMapping.getEmailColumn() < 0
                || columnMapping.getTicketColumn() < 0) {
            throw new CustomException(ExceptionCode.INVALID_SPREADSHEET_COLUMN_MAPPING);
        }
    }

    private void validateData(String email, String[] ticketNumbers) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CustomException(ExceptionCode.INVALID_EMAIL_FORMAT);
        }

        for (String ticketNumber : ticketNumbers) {
            if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
                continue;
            }
            if (!TICKET_NUMBER_PATTERN.matcher(ticketNumber).matches()) {
                throw new CustomException(ExceptionCode.INVALID_TICKET_NUMBER);
            }
        }
    }

    private String getCellValue(List<String> row, int columnIndex) {
        if (columnIndex >= row.size()) {
            return "";
        }
        return row.get(columnIndex).trim();
    }

    private boolean isRowEmpty(List<String> row) {
        for (String value : row) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public record SpreadsheetSource(String spreadsheetId, String sheetName, Integer sheetGid) {
    }

    public record SpreadsheetContent(
            String spreadsheetId,
            String spreadsheetTitle,
            String sheetTitle,
            Integer sheetGid,
            java.util.List<java.util.List<String>> rows
    ) {
    }

    private record SheetSelection(String title, Integer sheetGid) {
    }
}
