package com.example.aiec.repository;

import com.example.aiec.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 認証トークンリポジトリ
 */
@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    /**
     * トークンハッシュで検索
     */
    Optional<AuthToken> findByTokenHash(String tokenHash);

    /**
     * ユーザーの有効/失効トークンを取得
     */
    List<AuthToken> findByUserIdAndIsRevoked(Long userId, Boolean isRevoked);

    /**
     * 失効済みかつ期限切れのトークンを削除（定期クリーンアップ用）
     */
    void deleteByUserIdAndExpiresAtBeforeAndIsRevoked(Long userId, LocalDateTime expiresAt, Boolean isRevoked);

}
