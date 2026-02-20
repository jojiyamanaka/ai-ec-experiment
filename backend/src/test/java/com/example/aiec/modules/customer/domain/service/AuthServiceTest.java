package com.example.aiec.modules.customer.domain.service;

import com.example.aiec.modules.customer.domain.entity.AuthToken;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.repository.AuthTokenRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Captor
    private ArgumentCaptor<AuthToken> authTokenCaptor;

    private SimpleMeterRegistry meterRegistry;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        authService = new AuthService(authTokenRepository, meterRegistry);
        ReflectionTestUtils.invokeMethod(authService, "initMetrics");
    }

    @Test
    void createToken_shouldPersistHashedTokenAndReturnRawToken() {
        User user = new User();
        user.setId(10L);

        AuthService.TokenPair tokenPair = authService.createToken(user);

        verify(authTokenRepository).save(authTokenCaptor.capture());
        AuthToken savedToken = authTokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));

        assertThat(tokenPair.getRawToken()).isNotBlank();
        assertThat(tokenPair.getAuthToken()).isEqualTo(savedToken);
    }

    @Test
    void verifyToken_whenNotFound_shouldThrowUnauthorizedAndIncrementMetric() {
        when(authTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        double before = meterRegistry.counter("auth.login.failed", "type", "customer").count();

        assertThatThrownBy(() -> authService.verifyToken("missing-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("認証が必要です")
                .extracting("errorCode")
                .isEqualTo("UNAUTHORIZED");

        double after = meterRegistry.counter("auth.login.failed", "type", "customer").count();
        assertThat(after - before).isEqualTo(1.0d);
    }

    @Test
    void verifyToken_whenRevoked_shouldThrowUnauthorizedAndIncrementMetric() {
        AuthToken token = new AuthToken();
        token.setUser(new User());
        token.setIsRevoked(true);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(authTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        double before = meterRegistry.counter("auth.login.failed", "type", "customer").count();

        assertThatThrownBy(() -> authService.verifyToken("revoked-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("トークンが失効しています")
                .extracting("errorCode")
                .isEqualTo("UNAUTHORIZED");

        double after = meterRegistry.counter("auth.login.failed", "type", "customer").count();
        assertThat(after - before).isEqualTo(1.0d);
    }

    @Test
    void verifyToken_whenValid_shouldReturnUser() {
        User expectedUser = new User();
        expectedUser.setId(7L);

        AuthToken token = new AuthToken();
        token.setUser(expectedUser);
        token.setIsRevoked(false);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(authTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        User actual = authService.verifyToken("valid-token");

        assertThat(actual.getId()).isEqualTo(7L);
    }

    @Test
    void revokeToken_shouldMarkTokenAsRevoked() {
        AuthToken token = new AuthToken();
        token.setIsRevoked(false);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(authTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        authService.revokeToken("token");

        assertThat(token.getIsRevoked()).isTrue();
        verify(authTokenRepository).save(token);
    }
}
