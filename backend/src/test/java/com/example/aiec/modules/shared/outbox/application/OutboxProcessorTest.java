package com.example.aiec.modules.shared.outbox.application;

import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import com.example.aiec.modules.shared.outbox.domain.repository.OutboxEventRepository;
import com.example.aiec.modules.shared.outbox.handler.OutboxEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OutboxProcessor のポーリングロジック単体テスト。
 * OutboxProcessor は @Scheduled を持つが、ここでは process() を直接呼び出してテストする。
 */
@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxEventDispatcher dispatcher;

    // ── PENDING イベントなし ─────────────────────────────────────────────────

    @Test
    void process_noPendingEvents_shouldNotCallDispatcher() {
        when(outboxEventRepository.findPendingEvents(any(Instant.class))).thenReturn(List.of());

        OutboxProcessor processor = new OutboxProcessor(outboxEventRepository, dispatcher, List.of());
        processor.process();

        verifyNoInteractions(dispatcher);
    }

    // ── PENDING イベントあり ─────────────────────────────────────────────────

    @Test
    void process_withTwoPendingEvents_shouldCallDispatcherTwice() {
        OutboxEvent e1 = new OutboxEvent();
        e1.setEventType("TYPE_A");
        OutboxEvent e2 = new OutboxEvent();
        e2.setEventType("TYPE_B");
        when(outboxEventRepository.findPendingEvents(any(Instant.class))).thenReturn(List.of(e1, e2));

        OutboxProcessor processor = new OutboxProcessor(outboxEventRepository, dispatcher, List.of());
        processor.process();

        verify(dispatcher, times(2)).processOne(any(OutboxEvent.class), anyMap());
    }

    // ── ハンドラマップ構築 ──────────────────────────────────────────────────

    @Test
    void constructor_buildsHandlerMapFromHandlers() {
        OutboxEventHandler handlerA = mock(OutboxEventHandler.class);
        when(handlerA.getSupportedEventType()).thenReturn("TYPE_A");

        OutboxEvent event = new OutboxEvent();
        event.setEventType("TYPE_A");
        when(outboxEventRepository.findPendingEvents(any())).thenReturn(List.of(event));

        OutboxProcessor processor = new OutboxProcessor(outboxEventRepository, dispatcher, List.of(handlerA));
        processor.process();

        verify(dispatcher).processOne(eq(event), argThat(map -> map.containsKey("TYPE_A")));
    }
}
