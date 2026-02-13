package com.example.aiec.service;

import com.example.aiec.entity.BoAuthToken;
import com.example.aiec.entity.BoUser;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.BoAuthTokenRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoAuthService {
    private final BoAuthTokenRepository boAuthTokenRepository;
    private final BoUserService boUserService;

    /**
     * トークン生成
     */
    @Transactional
    public TokenPair createToken(BoUser boUser) {
        // 1. UUID v4 でランダムなトークンを生成
        String rawToken = UUID.randomUUID().toString();

        // 2. SHA-256 でハッシュ化
        String tokenHash = hashToken(rawToken);

        // 3. 有効期限を設定（7日間）
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // 4. bo_auth_tokens テーブルに保存
        BoAuthToken authToken = new BoAuthToken();
        authToken.setBoUserId(boUser.getId());
        authToken.setTokenHash(tokenHash);
        authToken.setExpiresAt(expiresAt);
        authToken.setIsRevoked(false);
        boAuthTokenRepository.save(authToken);

        // 5. 生トークンとハッシュのペアを返す
        return new TokenPair(rawToken, authToken);
    }

    /**
     * トークン検証
     */
    public BoUser verifyToken(String rawToken) {
        // 1. トークンをハッシュ化
        String tokenHash = hashToken(rawToken);

        // 2. bo_auth_tokens から検索
        BoAuthToken authToken = boAuthTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "無効なトークンです"));

        // 3. 失効チェック
        if (authToken.getIsRevoked()) {
            throw new BusinessException("TOKEN_REVOKED", "このトークンは失効しています");
        }

        // 4. 有効期限チェック
        if (authToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED", "トークンの有効期限が切れています");
        }

        // 5. BoUser を取得
        BoUser boUser = boUserService.findById(authToken.getBoUserId());

        // 6. 有効チェック
        if (!boUser.getIsActive()) {
            throw new ForbiddenException("BO_USER_INACTIVE", "このアカウントは無効化されています");
        }

        return boUser;
    }

    /**
     * トークン失効（ログアウト）
     */
    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        BoAuthToken authToken = boAuthTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "無効なトークンです"));
        authToken.setIsRevoked(true);
        boAuthTokenRepository.save(authToken);
    }

    /**
     * トークンをハッシュ化
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * トークンペア（生トークンとハッシュ）
     */
    @Data
    public static class TokenPair {
        private final String rawToken;
        private final BoAuthToken authToken;
    }
}
