package com.example.aiec.modules.shared.outbox.application;

import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import com.example.aiec.modules.shared.outbox.domain.repository.OutboxEventRepository;
import com.example.aiec.modules.shared.outbox.handler.OutboxEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Outboxイベントポーリングワーカー。
 * 5秒ごとに PENDING イベントを最大50件取得し OutboxEventDispatcher へ委譲する。
 */
@Service
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventDispatcher dispatcher;
    private final Map<String, OutboxEventHandler> handlerMap;

    public OutboxProcessor(
            OutboxEventRepository outboxEventRepository,
            OutboxEventDispatcher dispatcher,
            List<OutboxEventHandler> handlers) {
        this.outboxEventRepository = outboxEventRepository;
        this.dispatcher = dispatcher;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        OutboxEventHandler::getSupportedEventType,
                        Function.identity()
                ));
    }

    @Scheduled(fixedDelay = 5000)
    public void process() {
        List<OutboxEvent> events = outboxEventRepository.findPendingEvents(Instant.now());
        for (OutboxEvent event : events) {
            dispatcher.processOne(event, handlerMap);
        }
    }
}
