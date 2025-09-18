package com.example.mailsender.service.excel;

import com.example.mailsender.dto.TicketInfo;
import com.example.mailsender.exception.CustomException;
import com.example.mailsender.exception.ExceptionCode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ExcelService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private static final Pattern BOOKING_NUMBER_PATTERN =
            Pattern.compile("[0-9+]+");

    private static final int EMAIL_COLUMN = 1;
    private static final int NAME_COLUMN = 2;
    private static final int BOOKING_NUMBER_COLUMN = 3;


    public List<TicketInfo> parseExcel(MultipartFile file) throws IOException, CustomException {
        List<TicketInfo> tickets = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row)) continue;

                String email = formatter.formatCellValue(row.getCell(EMAIL_COLUMN)).trim();
                String name = formatter.formatCellValue(row.getCell(NAME_COLUMN)).trim();
                String bookingNoCell = formatter.formatCellValue(row.getCell(BOOKING_NUMBER_COLUMN)).trim();

                String[] bookingNumbers = bookingNoCell.split("\\+");
                validateData(email, bookingNumbers);

                List<Integer> bookingNoList = new ArrayList<>();
                for (String bookingNo : bookingNumbers) {
                    bookingNo = bookingNo.trim();
                    if (bookingNo.isEmpty()) continue;
                    bookingNoList.add(Integer.parseInt(bookingNo));
                }
                tickets.add(new TicketInfo(email, name, bookingNoList));
            }
            return tickets;
        }
    }

    private void validateData(String email, String[] bookingNoCell) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CustomException(ExceptionCode.INVALID_EMAIL_FORMAT);
        }

        for (String bookingNo : bookingNoCell) {
            if (!BOOKING_NUMBER_PATTERN.matcher(bookingNo).matches()) {
                throw new CustomException(ExceptionCode.INVALID_BOOKING_NUMBER);
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
