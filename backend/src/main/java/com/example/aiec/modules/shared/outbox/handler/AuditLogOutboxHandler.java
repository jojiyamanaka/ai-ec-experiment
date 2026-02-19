package com.example.aiec.modules.shared.outbox.handler;

import com.example.aiec.modules.shared.domain.entity.OperationHistory;
import com.example.aiec.modules.shared.domain.repository.OperationHistoryRepository;
import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * OPERATION_PERFORMED イベントの処理ハンドラ（監査ログ記録）。
 * OutboxEventDispatcher.processOne() とは別トランザクション（REQUIRES_NEW）で実行する。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogOutboxHandler implements OutboxEventHandler {

    private final OperationHistoryRepository operationHistoryRepository;

    @Override
    public String getSupportedEventType() {
        return "OPERATION_PERFORMED";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(OutboxEvent event) throws Exception {
        JsonNode payload = event.getPayload();

        OperationHistory history = new OperationHistory();
        history.setOperationType(payload.path("operationType").asText());
        history.setPerformedBy(payload.path("performedBy").asText());
        history.setRequestPath(payload.path("requestPath").asText());
        history.setDetails(payload.path("details").asText());

        operationHistoryRepository.save(history);
        log.debug("監査ログ記録完了: outboxId={}, type={}", event.getId(), history.getOperationType());
    }
}
