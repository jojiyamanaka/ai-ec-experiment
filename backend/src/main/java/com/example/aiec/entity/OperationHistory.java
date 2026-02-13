package com.example.aiec.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作履歴エンティティ
 */
@Entity
@Table(name = "operation_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * イベント種別（LOGIN_SUCCESS, LOGIN_FAILURE, AUTHORIZATION_ERROR, ADMIN_ACTION）
     */
    @Column(nullable = false, length = 50)
    private String eventType;

    /**
     * イベント詳細（JSONまたは自由形式テキスト）
     */
    @Column(nullable = false, length = 500)
    private String details;

    /**
     * 対象ユーザーID（ログイン失敗時はnullの場合あり）
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 対象ユーザーのメールアドレス（ログイン失敗時の記録用）
     */
    @Column(length = 255)
    private String userEmail;

    /**
     * IPアドレス（将来拡張用、現在はnull）
     */
    @Column(length = 45)
    private String ipAddress;

    /**
     * リクエストパス（例: /api/order/123/ship）
     */
    @Column(length = 255)
    private String requestPath;

    /**
     * 発生日時
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
