package com.example.mailsender.service.template;

import com.example.mailsender.dto.Template;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

@Service
public class TemplateService {
    private static final String TEMPLATE_FILE_SUFFIX = "-template.json";

    private final ObjectMapper objectMapper;
    private final UserScopedFileStorageService userScopedFileStorageService;

    public TemplateService(
            ObjectMapper objectMapper,
            UserScopedFileStorageService userScopedFileStorageService
    ) {
        this.objectMapper = objectMapper;
        this.userScopedFileStorageService = userScopedFileStorageService;
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
        return userScopedFileStorageService.resolveCurrentUserFile(TEMPLATE_FILE_SUFFIX);
    }

    private record StoredTemplate(String subject, String body, String fileName, String fileDataBase64) {}
}
