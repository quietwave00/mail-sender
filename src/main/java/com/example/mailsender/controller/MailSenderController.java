package com.example.mailsender.controller;

import com.example.mailsender.dto.Template;
import com.example.mailsender.dto.TicketInfo;
import com.example.mailsender.dto.request.SendMailRequest;
import com.example.mailsender.dto.response.MailPreviewListResponse;
import com.example.mailsender.service.excel.ExcelService;
import com.example.mailsender.service.mail.MailService;
import com.example.mailsender.service.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MailSenderController {

    private final TemplateService templateService;
    private final ExcelService excelService;
    private final MailService mailService;

    @PostMapping("/template")
    public ResponseEntity<?> setTemplate(
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            @RequestPart(value = "templateFile", required = false) MultipartFile templateFile,
            @RequestParam(value = "existingFileName", required = false) String existingFileName,
            @RequestParam(value = "existingFileData", required = false) String existingFileData,
            @RequestParam(value = "useExistingFile", required = false, defaultValue = "false") boolean useExistingFile) {
        try {
            if (useExistingFile) {
                templateService.setTemplate(subject, body, existingFileName, existingFileData);
            } else {
                templateService.setTemplate(subject, body, templateFile);
            }
            return ResponseEntity.ok("템플릿 설정 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("템플릿 설정 실패: " + e.getMessage());
        }
    }

    @GetMapping("/template")
    public ResponseEntity<?> getTemplate() {
        try {
            Template template = templateService.getTemplate();
            if (template == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("템플릿 설정 실패: " + e.getMessage());
        }
    }

    @PostMapping("/mail")
    public ResponseEntity<?> sendMail(@ModelAttribute SendMailRequest request) {
        try {
            List<TicketInfo> tickets = excelService.parseExcel(request);
            mailService.sendMails(tickets);
            return ResponseEntity.ok("메일 전송 완료! " + tickets.size() + "명에게 발송");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("에러: " + e.getMessage());
        }
    }

    @PostMapping("/mail/preview")
    public ResponseEntity<?> getMailList(@ModelAttribute SendMailRequest request) throws IOException {
        MailPreviewListResponse response = mailService.previewMails(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mail/progress")
    public ResponseEntity<?> getMailProgress() throws IOException {
        int response = mailService.getMailProgress();
        return ResponseEntity.ok(response);
    }
}
