package com.example.aiec.modules.shared.outbox.application;

import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent.OutboxStatus;
import com.example.aiec.modules.shared.outbox.domain.repository.OutboxEventRepository;
import com.example.aiec.modules.shared.outbox.handler.OutboxEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Outboxイベントの個別処理（トランザクション分離のため OutboxProcessor から切り出し）。
 * processOne() を別クラスに置くことで Spring AOP が @Transactional を正常にインターセプトする。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventDispatcher {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void processOne(OutboxEvent event, Map<String, OutboxEventHandler> handlerMap) {
        event.setStatus(OutboxStatus.PROCESSING);
        outboxEventRepository.save(event);

        OutboxEventHandler handler = handlerMap.get(event.getEventType());
        if (handler == null) {
            log.warn("未知のイベントタイプ: {}", event.getEventType());
            event.setStatus(OutboxStatus.DEAD);
            event.setErrorMessage("No handler for event type: " + event.getEventType());
            outboxEventRepository.save(event);
            return;
        }

        try {
            handler.handle(event);
            event.setStatus(OutboxStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            log.info("Outboxイベント処理完了: id={}, type={}", event.getId(), event.getEventType());
        } catch (Exception e) {
            int newRetryCount = event.getRetryCount() + 1;
            event.setRetryCount(newRetryCount);
            event.setErrorMessage(e.getMessage());

            if (newRetryCount >= event.getMaxRetries()) {
                event.setStatus(OutboxStatus.DEAD);
                log.error("Outboxイベント最大リトライ到達（DEAD）: id={}, type={}",
                        event.getId(), event.getEventType(), e);
            } else {
                event.setStatus(OutboxStatus.PENDING);
                event.setScheduledAt(Instant.now().plusSeconds(30L * newRetryCount));
                log.warn("Outboxイベント再試行スケジュール: id={}, type={}, retryCount={}/{}",
                        event.getId(), event.getEventType(), newRetryCount, event.getMaxRetries(), e);
            }
        }
        outboxEventRepository.save(event);
    }
}
