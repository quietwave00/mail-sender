package com.example.mailsender.service.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CurrentUserService {

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("로그인한 사용자 정보를 찾을 수 없습니다.");
        }

        return authentication.getName().trim().toLowerCase(Locale.ROOT);
    }
}
