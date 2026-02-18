package com.example.aiec.modules.customer.domain.repository;

import com.example.aiec.modules.shared.domain.model.ActorType;
import com.example.aiec.modules.customer.domain.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
    void deleteByUserIdAndExpiresAtBeforeAndIsRevoked(Long userId, Instant expiresAt, Boolean isRevoked);

    @Modifying
    @Query("UPDATE AuthToken t SET t.isDeleted = TRUE, t.deletedAt = CURRENT_TIMESTAMP, t.deletedByType = :deletedByType, t.deletedById = :deletedById WHERE t.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);

}
