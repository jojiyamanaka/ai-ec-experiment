package com.example.aiec.modules.customer.domain.service;

import com.example.aiec.modules.customer.domain.entity.AuthToken;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.customer.domain.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 認証サービス
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthTokenRepository authTokenRepository;
    private static final int TOKEN_EXPIRATION_DAYS = 7;

    /**
     * トークン生成
     * @return AuthToken と生のトークン文字列のペア
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenPair createToken(User user) {
        // 1. 生のトークン（UUID）を生成
        String rawToken = UUID.randomUUID().toString();

        // 2. ハッシュ化してDBに保存
        String tokenHash = hashToken(rawToken);

        AuthToken authToken = new AuthToken();
        authToken.setUser(user);
        authToken.setTokenHash(tokenHash);
        authToken.setExpiresAt(Instant.now().plus(TOKEN_EXPIRATION_DAYS, ChronoUnit.DAYS));
        authTokenRepository.save(authToken);

        // 3. 生のトークンをクライアントに返すため、ペアで返す
        return new TokenPair(authToken, rawToken);
    }

    /**
     * トークン検証
     * @param rawToken クライアントから受け取った生のトークン
     * @return 認証済みユーザー
     */
    public User verifyToken(String rawToken) {
        // 1. トークンをハッシュ化
        String tokenHash = hashToken(rawToken);

        // 2. ハッシュ値でDB検索
        AuthToken authToken = authTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "認証が必要です"));

        // 3. 有効性チェック（期限切れ・失効）
        if (!authToken.isValid()) {
            throw new BusinessException("UNAUTHORIZED",
                    authToken.getIsRevoked() ? "トークンが失効しています" : "トークンの有効期限が切れています");
        }

        return authToken.getUser();
    }

    /**
     * トークン失効（ログアウト）
     * @param rawToken クライアントから受け取った生のトークン
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        AuthToken authToken = authTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "認証が必要です"));

        authToken.revoke();
        authTokenRepository.save(authToken);
    }

    /**
     * トークンをSHA-256でハッシュ化
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * トークンと生の文字列のペア
     */
    @Value
    public static class TokenPair {
        AuthToken authToken;
        String rawToken;
    }

}
