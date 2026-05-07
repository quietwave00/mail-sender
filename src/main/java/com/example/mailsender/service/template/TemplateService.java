package com.example.mailsender.service.template;

import com.example.mailsender.dto.Template;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

@Service
public class TemplateService {
    private static final String TEMPLATE_FILE_SUFFIX = "-template.json";

    private final ObjectMapper objectMapper;
    private final Path storageDir;

    public TemplateService(
            ObjectMapper objectMapper,
            @Value("${app.template.storage-dir:/data}") String storageDir) throws IOException {
        this.objectMapper = objectMapper;
        this.storageDir = Paths.get(storageDir);
        Files.createDirectories(this.storageDir);
    }

    public void setTemplate(String subject, String body, MultipartFile templateFile) throws IOException {
        synchronized (this) {
            writeToDisk(new Template(subject, body, templateFile));
        }
    }

    public void setTemplate(String subject, String body, String existingFileName, String existingFileData) {
        synchronized (this) {
            writeToDisk(new Template(subject, body, existingFileData, existingFileName));
        }
    }

    public Template getTemplate() {
        return readFromDisk();
    }

    public Boolean hasFile() {
        Template current = getTemplate();
        return Objects.nonNull(current) && Objects.nonNull(current.getTemplateFileData());
    }

    public byte[] getFile() {
        Template current = getTemplate();
        return Objects.isNull(current) ? null : current.getTemplateFileData();
    }

    private Template readFromDisk() {
        Path storageFile = resolveStorageFile();
        if (!Files.exists(storageFile)) {
            return null;
        }

        try {
            StoredTemplate stored = objectMapper.readValue(storageFile.toFile(), StoredTemplate.class);
            if (stored == null) return null;
            return new Template(
                    stored.subject(),
                    stored.body(),
                    stored.fileDataBase64(),
                    stored.fileName()
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load template", e);
        }
    }

    private void writeToDisk(Template template) {
        if (template == null) {
            return;
        }

        Path storageFile = resolveStorageFile();

        StoredTemplate stored = new StoredTemplate(
                template.getSubject(),
                template.getBody(),
                template.getTemplateFileName(),
                template.getTemplateFileData() != null
                        ? Base64.getEncoder().encodeToString(template.getTemplateFileData())
                        : null
        );

        try {
            objectMapper.writeValue(storageFile.toFile(), stored);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save template", e);
        }
    }

    private Path resolveStorageFile() {
        String userEmail = getCurrentUserEmail();
        return storageDir.resolve(toStorageFileName(userEmail));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("로그인한 사용자 정보를 찾을 수 없습니다.");
        }

        return authentication.getName().trim().toLowerCase(Locale.ROOT);
    }

    private String toStorageFileName(String email) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return encodedEmail + TEMPLATE_FILE_SUFFIX;
    }

    private record StoredTemplate(String subject, String body, String fileName, String fileDataBase64) {}
}
