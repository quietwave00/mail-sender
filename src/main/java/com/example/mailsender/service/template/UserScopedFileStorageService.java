package com.example.mailsender.service.template;

import com.example.mailsender.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UserScopedFileStorageService {
    private final Path storageDir;
    private final CurrentUserService currentUserService;

    public UserScopedFileStorageService(
            @Value("${app.template.storage-dir:/data}") String storageDir,
            CurrentUserService currentUserService
    ) {
        this.storageDir = Paths.get(storageDir);
        this.currentUserService = currentUserService;
        createStorageDirectory();
    }

    public Path resolveCurrentUserFile(String fileSuffix) {
        return storageDir.resolve(toFileName(currentUserService.getCurrentUserEmail(), fileSuffix));
    }

    private String toFileName(String email, String fileSuffix) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return encodedEmail + fileSuffix;
    }

    private void createStorageDirectory() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create storage directory", e);
        }
    }
}
