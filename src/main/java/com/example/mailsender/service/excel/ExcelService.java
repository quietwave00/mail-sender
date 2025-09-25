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
        List<TicketInfo> tickets = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream is = request.getFile().getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row)) continue;

                String email = formatter.formatCellValue(row.getCell(request.getEmailColumn())).trim();
                String name = formatter.formatCellValue(row.getCell(request.getNameColumn())).trim();
                String ticketNumberCell = formatter.formatCellValue(row.getCell(request.getTicketColumn())).trim();

                String[] ticketNumbers = ticketNumberCell.split("\\+");
                validateData(email, ticketNumbers);

                List<Integer> ticketNumberList = new ArrayList<>();
                for (String ticketNumber : ticketNumbers) {
                    ticketNumber = ticketNumber.trim();
                    if (ticketNumber.isEmpty()) continue;
                    ticketNumberList.add(Integer.parseInt(ticketNumber));
                }
                tickets.add(new TicketInfo(email, name, ticketNumberList));
            }
            return tickets;
        }
    }

    private void validateData(String email, String[] ticketNumbers) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CustomException(ExceptionCode.INVALID_EMAIL_FORMAT);
        }

        for (String ticketNumber : ticketNumbers) {
            if (!TICKET_NUMBER_PATTERN.matcher(ticketNumber).matches()) {
                throw new CustomException(ExceptionCode.INVALID_TICKET_NUMBER);
            }
        }
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
