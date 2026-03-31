package com.example.mailsender.service.excel;

import com.example.mailsender.dto.TicketInfo;
import com.example.mailsender.dto.request.SendMailRequest;
import com.example.mailsender.exception.CustomException;
import com.example.mailsender.exception.ExceptionCode;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ExcelService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private static final Pattern TICKET_NUMBER_PATTERN =
            Pattern.compile("[0-9+]+");


    public List<TicketInfo> parseExcel(SendMailRequest request) throws IOException, CustomException {
        Integer fromValue = request.getFromValue();
        Integer toValue = request.getToValue();
        validateSendRange(fromValue, toValue);

        DataFormatter formatter = new DataFormatter();
        try (InputStream is = request.getFile().getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (hasSeparateSendRange(fromValue, toValue)) {
                return parseSeparateSendRows(sheet, formatter, request, fromValue, toValue);
            }
            return parseAllRows(sheet, formatter, request);
        }
    }

    private boolean hasSeparateSendRange(Integer fromValue, Integer toValue) {
        return fromValue != null && toValue != null;
    }

    private List<TicketInfo> parseAllRows(Sheet sheet, DataFormatter formatter, SendMailRequest request) {
        int firstDataRow = 1;
        int lastDataRow = sheet.getPhysicalNumberOfRows();
        return collectTickets(sheet, formatter, request, firstDataRow, lastDataRow);
    }

    private List<TicketInfo> parseSeparateSendRows(
            Sheet sheet,
            DataFormatter formatter,
            SendMailRequest request,
            int fromValue,
            int toValue
    ) {
        int lastDataRow = sheet.getPhysicalNumberOfRows();
        int adjustedToValue = Math.min(toValue, lastDataRow);
        if (fromValue > adjustedToValue) {
            return new ArrayList<>();
        }
        return collectTickets(sheet, formatter, request, fromValue, adjustedToValue);
    }

    private List<TicketInfo> collectTickets(
            Sheet sheet,
            DataFormatter formatter,
            SendMailRequest request,
            int startRow,
            int endRow
    ) {
        List<TicketInfo> tickets = new ArrayList<>();
        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            TicketInfo ticketInfo = parseTicketRow(sheet.getRow(rowIndex), formatter, request);
            if (ticketInfo != null) {
                tickets.add(ticketInfo);
            }
        }
        return tickets;
    }

    private TicketInfo parseTicketRow(Row row, DataFormatter formatter, SendMailRequest request) {
        if (isRowEmpty(row)) {
            return null;
        }

        String email = getTrimmedCellValue(row, request.getEmailColumn(), formatter);
        String name = getTrimmedCellValue(row, request.getNameColumn(), formatter);
        String ticketNumberCell = getTrimmedCellValue(row, request.getTicketColumn(), formatter);

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

        return new TicketInfo(email, name, ticketNumberList);
    }

    private void validateSendRange(Integer fromValue, Integer toValue) {
        if (fromValue == null && toValue == null) {
            return;
        }

        if (fromValue == null || toValue == null) {
            throw new CustomException(ExceptionCode.INVALID_SEND_RANGE);
        }

        if (fromValue <= 0 || toValue <= 0 || fromValue > toValue) {
            throw new CustomException(ExceptionCode.INVALID_SEND_RANGE);
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

    private String getTrimmedCellValue(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = cell.toString().trim();
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
