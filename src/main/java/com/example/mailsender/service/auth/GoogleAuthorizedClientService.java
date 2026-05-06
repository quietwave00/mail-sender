package com.example.mailsender.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthorizedClientService {
    private static final String GOOGLE_REGISTRATION_ID = "google";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2AuthorizedClient getAuthorizedClient(Authentication authentication) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(GOOGLE_REGISTRATION_ID)
                .principal(authentication)
                .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
        if (authorizedClient == null) {
            throw new IllegalStateException("구글 OAuth2 토큰이 없습니다. 다시 로그인하세요.");
        }

        return authorizedClient;
    }
}
