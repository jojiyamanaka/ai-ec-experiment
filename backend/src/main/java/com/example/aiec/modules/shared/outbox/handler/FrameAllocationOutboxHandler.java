package com.example.aiec.modules.shared.outbox.handler;

import com.example.aiec.modules.inventory.application.service.FrameAllocationService;
import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注文作成イベントを契機に枠在庫商品の本引当を再試行する。
 */
@Component
@RequiredArgsConstructor
public class FrameAllocationOutboxHandler implements OutboxEventHandler {

    private final FrameAllocationService frameAllocationService;

    @Override
    public String getSupportedEventType() {
        return "ORDER_PLACED";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(OutboxEvent event) {
        Long orderId = event.getPayload().path("orderId").asLong();
        if (orderId > 0) {
            frameAllocationService.allocatePendingByOrderId(orderId);
        }
    }
}
