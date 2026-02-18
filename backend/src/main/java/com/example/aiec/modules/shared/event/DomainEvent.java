package com.example.aiec.modules.shared.event;

import java.time.Instant;

/**
 * ドメインイベントの基底インターフェース
 */
public interface DomainEvent {

    /**
     * イベント発生時刻
     */
    Instant occurredAt();

}
