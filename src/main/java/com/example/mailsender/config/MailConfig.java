package com.example.mailsender.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class MailConfig {

    private final OAuth2AuthorizedClientService clientService;


    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        // 기본 프로퍼티 세팅
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "false");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }

    /**
     * 현재 로그인된 사용자의 accessToken을 이용해 발신자 계정 세팅
     */
    public void applyOAuth2Authentication(JavaMailSenderImpl mailSender, String principalName) {
        OAuth2AuthorizedClient client =
                clientService.loadAuthorizedClient("google", principalName);

        if (client == null) {
            throw new IllegalStateException("구글 OAuth2 토큰이 없습니다. 다시 로그인하세요.");
        }

        String accessToken = client.getAccessToken().getTokenValue();
        String email = client.getPrincipalName();

        mailSender.setUsername(email);
        mailSender.setPassword(accessToken);
    }
}