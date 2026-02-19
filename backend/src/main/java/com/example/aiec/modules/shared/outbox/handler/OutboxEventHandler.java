package com.example.aiec.modules.shared.outbox.handler;

import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;

public interface OutboxEventHandler {
    String getSupportedEventType();
    void handle(OutboxEvent event) throws Exception;
}
