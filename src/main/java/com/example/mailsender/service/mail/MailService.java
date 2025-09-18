package com.example.mailsender.service.mail;

import com.example.mailsender.config.MailConfig;
import com.example.mailsender.dto.MailPreview;
import com.example.mailsender.dto.Template;
import com.example.mailsender.dto.TicketInfo;
import com.example.mailsender.dto.response.MailPreviewListResponse;
import com.example.mailsender.service.excel.ExcelService;
import com.example.mailsender.service.template.TemplateProcessor;
import com.example.mailsender.service.template.TemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateService templateService;
    private final ExcelService excelService;
    private final MailConfig mailConfig;

    public void sendMails(List<TicketInfo> tickets) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        tickets.forEach(ticket -> {
            sendMail(ticket, auth.getName());
        });
    }

    private void sendMail(TicketInfo ticket, String principalName) {
        Template template = templateService.getTemplate();
        Map<String, String> variables = createVariableMap(ticket);
        String processedBody = TemplateProcessor.processTemplate(template.getBody(), variables);

        JavaMailSenderImpl senderImpl = (JavaMailSenderImpl) mailSender;
        mailConfig.applyOAuth2Authentication(senderImpl, principalName);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            mimeMessageHelper.setFrom(senderImpl.getUsername());
            mimeMessageHelper.setTo(ticket.getEmail());
            mimeMessageHelper.setSubject(template.getSubject());
            mimeMessageHelper.setText(processedBody, true);
            if(templateService.hasFile()) {
                byte[] fileData = template.getTemplateFileData();
                String fileName = template.getTemplateFileName();
                if (fileData != null && fileName != null && !fileName.isEmpty()) {
                    mimeMessageHelper.addAttachment(fileName, new ByteArrayResource(fileData));
                }
            }
            mailSender.send(mimeMessage);

        } catch(MessagingException e) {
            throw new RuntimeException("메일 발송에 실패했습니다.", e);
        }
    }

    private Map<String, String> createVariableMap(TicketInfo ticket) {
        Map<String, String> variables = new HashMap<>();

        variables.put("이름", ticket.getName());
        variables.put("티켓번호", ticket.getBookingNo().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")));

        return variables;
    }

    public MailPreviewListResponse previewMails(MultipartFile file) throws IOException {
        List<TicketInfo> tickets = excelService.parseExcel(file);
        Template template = templateService.getTemplate();

        List<MailPreview> previews = tickets.stream()
                .map(ticket -> createMailPreview(ticket, template))
                .collect(Collectors.toList());

        return new MailPreviewListResponse(
                previews,
                tickets.size(),
                template.getSubject(),
                template.getBody(),
                template.getTemplateFileData(),
                template.getTemplateFileName()
        );
    }

    private MailPreview createMailPreview(TicketInfo ticket, Template template) {
        Map<String, String> variables = createVariableMap(ticket);
        String ticketNumbers = variables.get("티켓번호");

        return new MailPreview(
                ticket.getEmail(),
                ticket.getName(),
                ticketNumbers
        );
    }
}
