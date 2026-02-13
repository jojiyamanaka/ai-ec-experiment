package com.example.aiec.repository;

import com.example.aiec.entity.ActorType;
import com.example.aiec.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ユーザーリポジトリ
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * メールアドレスでユーザーを検索
     */
    Optional<User> findByEmail(String email);

    /**
     * メールアドレスの存在確認
     */
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.isDeleted = TRUE, u.deletedAt = CURRENT_TIMESTAMP, u.deletedByType = :deletedByType, u.deletedById = :deletedById WHERE u.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);

}
