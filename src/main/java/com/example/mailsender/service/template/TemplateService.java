package com.example.mailsender.service.template;

import com.example.mailsender.dto.Template;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;

@Service
public class TemplateService
{
    private final ObjectMapper objectMapper;
    private final Path storageFile;
    private Template template;

    public TemplateService(
            ObjectMapper objectMapper,
            @Value("${app.template.storage-dir:/data}") String storageDir) throws IOException {
        this.objectMapper = objectMapper;
        Path dir = Paths.get(storageDir);
        Files.createDirectories(dir);
        this.storageFile = dir.resolve("template.json");
    }

    @PostConstruct
    void loadFromDisk() {
        this.template = readFromDisk();
    }

    public void setTemplate(String subject, String body, MultipartFile templateFile) throws IOException {
        synchronized (this) {
            this.template = new Template(subject, body, templateFile);
            writeToDisk(this.template);
        }
    }

    public void setTemplate(String subject, String body, String existingFileName, String existingFileData) {
        synchronized (this) {
            this.template = new Template(subject, body, existingFileData, existingFileName);
            writeToDisk(this.template);
        }
    }

    public Template getTemplate() {
        synchronized (this) {
            if (template == null) {
                template = readFromDisk();
            }
            return template;
        }
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

    private record StoredTemplate(String subject, String body, String fileName, String fileDataBase64) {}
}
