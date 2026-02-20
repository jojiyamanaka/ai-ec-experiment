package com.example.aiec.modules.backoffice.domain.service;

import com.example.aiec.modules.backoffice.domain.entity.BoAuthToken;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.repository.BoAuthTokenRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoAuthServiceTest {

    @Mock
    private BoAuthTokenRepository boAuthTokenRepository;

    @Mock
    private BoUserService boUserService;

    private BoAuthService boAuthService;

    @BeforeEach
    void setUp() {
        boAuthService = new BoAuthService(boAuthTokenRepository, boUserService);
    }

    @Test
    void verifyToken_whenTokenNotFound_shouldThrowInvalidToken() {
        when(boAuthTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boAuthService.verifyToken("missing"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("無効なトークンです")
                .extracting("errorCode")
                .isEqualTo("INVALID_TOKEN");
    }

    @Test
    void verifyToken_whenRevoked_shouldThrowTokenRevoked() {
        BoAuthToken token = token(false, true);
        when(boAuthTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> boAuthService.verifyToken("revoked"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("このトークンは失効しています")
                .extracting("errorCode")
                .isEqualTo("TOKEN_REVOKED");
    }

    @Test
    void verifyToken_whenExpired_shouldThrowTokenExpired() {
        BoAuthToken token = token(true, false);
        when(boAuthTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> boAuthService.verifyToken("expired"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("トークンの有効期限が切れています")
                .extracting("errorCode")
                .isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void verifyToken_whenInactiveUser_shouldThrowForbidden() {
        BoAuthToken token = token(false, false);
        BoUser inactive = new BoUser();
        inactive.setId(10L);
        inactive.setIsActive(false);

        when(boAuthTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(boUserService.findById(10L)).thenReturn(inactive);

        assertThatThrownBy(() -> boAuthService.verifyToken("inactive"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("このアカウントは無効化されています");
    }

    @Test
    void verifyToken_whenValid_shouldReturnBoUser() {
        BoAuthToken token = token(false, false);
        BoUser active = new BoUser();
        active.setId(10L);
        active.setIsActive(true);

        when(boAuthTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(boUserService.findById(10L)).thenReturn(active);

        BoUser actual = boAuthService.verifyToken("valid");

        assertThat(actual.getId()).isEqualTo(10L);
    }

    @Test
    void revokeToken_shouldMarkRevokedAndPersist() {
        BoAuthToken token = token(false, false);
        when(boAuthTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        boAuthService.revokeToken("token");

        assertThat(token.getIsRevoked()).isTrue();
        verify(boAuthTokenRepository).save(token);
    }

    private BoAuthToken token(boolean expired, boolean revoked) {
        BoAuthToken token = new BoAuthToken();
        token.setBoUserId(10L);
        token.setIsRevoked(revoked);
        token.setExpiresAt(expired ? Instant.now().minus(1, ChronoUnit.MINUTES) : Instant.now().plus(1, ChronoUnit.DAYS));
        return token;
    }
}
