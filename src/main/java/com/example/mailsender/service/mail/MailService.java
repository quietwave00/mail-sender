package com.example.mailsender.service.mail;

import com.example.mailsender.config.MailConfig;
import com.example.mailsender.dto.MailPreview;
import com.example.mailsender.dto.Template;
import com.example.mailsender.dto.TicketInfo;
import com.example.mailsender.dto.request.SendMailRequest;
import com.example.mailsender.dto.response.MailPreviewListResponse;
import com.example.mailsender.service.excel.ExcelService;
import com.example.mailsender.service.template.TemplateProcessor;
import com.example.mailsender.service.template.TemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateService templateService;
    private final ExcelService excelService;
    private final MailConfig mailConfig;

    private final AtomicInteger sentCount = new AtomicInteger(0);

    public void sendMails(List<TicketInfo> tickets) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
        mailConfig.applyOAuth2Authentication(sender, auth.getName());

        Session session = sender.getSession();
        Transport transport = null;

        try {
            transport = session.getTransport("smtp");

            transport.connect(
                    sender.getHost(),
                    sender.getPort(),
                    sender.getUsername(),
                    sender.getPassword()
            );

            for (TicketInfo ticket : tickets) {
                sendMailWithTransport(ticket, transport, session, sender.getUsername());
                sentCount.incrementAndGet();
            }

        } catch (Exception e) {
            throw new RuntimeException("메일 발송 중 오류", e);
        } finally {
            if (transport != null && transport.isConnected()) {
                try {
                    transport.close();
                } catch (MessagingException ignored) {}
            }
        }
    }

    private void sendMailWithTransport(
            TicketInfo ticket,
            Transport transport,
            Session session,
            String fromEmail
    ) throws MessagingException {

        Template template = templateService.getTemplate();
        Map<String, String> variables = createVariableMap(ticket);
        String processedBody =
                TemplateProcessor.processTemplate(template.getBody(), variables);

        MimeMessage message = new MimeMessage(session);
        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(ticket.getEmail());
        helper.setSubject(template.getSubject());
        helper.setText(processedBody, true);

        if (templateService.hasFile()) {
            byte[] fileData = template.getTemplateFileData();
            String fileName = template.getTemplateFileName();
            if (fileData != null && fileName != null && !fileName.isEmpty()) {
                helper.addAttachment(fileName, new ByteArrayResource(fileData));
            }
        }

        transport.sendMessage(message, message.getAllRecipients());
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
            sentCount.incrementAndGet();
        } catch(MessagingException e) {
            throw new RuntimeException("메일 발송에 실패했습니다.", e);
        }
    }

    public MailPreviewListResponse previewMails(SendMailRequest request) throws IOException {
        List<TicketInfo> tickets = excelService.parseExcel(request);
        Template template = templateService.getTemplate();

        List<MailPreview> previews = tickets.stream()
                .map(this::createMailPreview)
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

    public int getMailProgress() {
        return sentCount.get();
    }

    /**
     * private
     */

    private Map<String, String> createVariableMap(TicketInfo ticket) {
        Map<String, String> variables = new HashMap<>();

        variables.put("이름", ticket.getName());
        variables.put("예매번호", ticket.getTicketNumbers().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")));
        variables.put("매수", String.valueOf(ticket.getTicketCount()));

        return variables;
    }

    private MailPreview createMailPreview(TicketInfo ticket) {
        Map<String, String> variables = createVariableMap(ticket);
        String ticketNumbers = variables.get("예매번호");
        String ticketCount = variables.get("매수");

        return new MailPreview(
                ticket.getEmail(),
                ticket.getName(),
                ticketNumbers,
                ticketCount
        );
    }
}
