package com.example.aiec.modules.shared.event;

import java.time.Instant;

/**
 * 操作実行イベント（監査ログ記録用）
 *
 * 本処理が失敗してもログを残すため、イベントハンドラは別トランザクション（REQUIRES_NEW）で実行される
 */
public record OperationPerformedEvent(
    String operationType,
    String performedBy,
    String requestPath,
    String details,
    Instant occurredAt
) implements DomainEvent {

    /**
     * コンストラクタ（occurredAt を自動設定）
     */
    public OperationPerformedEvent(
        String operationType,
        String performedBy,
        String requestPath,
        String details
    ) {
        this(operationType, performedBy, requestPath, details, Instant.now());
    }

}
