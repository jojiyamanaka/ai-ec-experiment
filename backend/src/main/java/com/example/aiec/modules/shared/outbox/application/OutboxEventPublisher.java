package com.example.aiec.modules.shared.outbox.application;

import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import com.example.aiec.modules.shared.outbox.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Outboxへのイベント書き込みサービス。
 * 呼び出し元のトランザクション内で実行されるため、メイン処理と同一トランザクションに参加する。
 */
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String aggregateId, Object payloadObject) {
        com.fasterxml.jackson.databind.JsonNode payload = objectMapper.valueToTree(payloadObject);
        OutboxEvent event = OutboxEvent.create(eventType, aggregateId, payload);
        outboxEventRepository.save(event);
    }
}
