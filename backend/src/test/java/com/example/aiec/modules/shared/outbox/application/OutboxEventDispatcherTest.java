package com.example.aiec.modules.shared.outbox.application;

import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent.OutboxStatus;
import com.example.aiec.modules.shared.outbox.domain.repository.OutboxEventRepository;
import com.example.aiec.modules.shared.outbox.handler.OutboxEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OutboxEventDispatcher の単体テスト。
 * JPA コンテキストなし。processOne() の4つのパスを網羅する。
 */
@ExtendWith(MockitoExtension.class)
class OutboxEventDispatcherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventDispatcher dispatcher;

    // ── ハンドラ未登録 ─────────────────────────────────────────────────────────

    @Test
    void processOne_noHandler_shouldMarkDead() {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("UNKNOWN_TYPE");

        dispatcher.processOne(event, Map.of());

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(event.getErrorMessage()).contains("UNKNOWN_TYPE");
        // PROCESSING への更新 + DEAD への更新 = 2回
        verify(outboxEventRepository, times(2)).save(event);
    }

    // ── ハンドラ成功 ──────────────────────────────────────────────────────────

    @Test
    void processOne_handlerSucceeds_shouldMarkProcessed() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("TEST_EVENT");
        OutboxEventHandler handler = mock(OutboxEventHandler.class);

        dispatcher.processOne(event, Map.of("TEST_EVENT", handler));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(handler).handle(event);
    }

    // ── ハンドラ失敗・リトライ圏内 ─────────────────────────────────────────────

    @Test
    void processOne_handlerFails_belowMaxRetries_shouldScheduleRetry() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("TEST_EVENT");
        event.setRetryCount(0);
        event.setMaxRetries(3);
        OutboxEventHandler handler = mock(OutboxEventHandler.class);
        doThrow(new RuntimeException("handler error")).when(handler).handle(any());

        dispatcher.processOne(event, Map.of("TEST_EVENT", handler));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getScheduledAt()).isNotNull();
        assertThat(event.getErrorMessage()).isEqualTo("handler error");
    }

    // ── ハンドラ失敗・最大リトライ到達 ─────────────────────────────────────────

    @Test
    void processOne_handlerFails_atMaxRetries_shouldMarkDead() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("TEST_EVENT");
        event.setRetryCount(2);
        event.setMaxRetries(3);
        OutboxEventHandler handler = mock(OutboxEventHandler.class);
        doThrow(new RuntimeException("final error")).when(handler).handle(any());

        dispatcher.processOne(event, Map.of("TEST_EVENT", handler));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(event.getRetryCount()).isEqualTo(3);
    }
}
