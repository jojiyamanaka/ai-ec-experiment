package com.example.aiec.modules.shared.outbox.handler;

import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * ORDER_CONFIRMED イベントの処理ハンドラ（注文確認メール送信）。
 * customerEmail が null のゲスト注文はスキップ。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxHandler implements OutboxEventHandler {

    private final JavaMailSender mailSender;

    @Override
    public String getSupportedEventType() {
        return "ORDER_CONFIRMED";
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        JsonNode payload = event.getPayload();
        String customerEmail = payload.path("customerEmail").asText(null);

        if (customerEmail == null || customerEmail.isBlank()) {
            log.debug("ゲスト注文のためメール送信スキップ: outboxId={}", event.getId());
            return;
        }

        String orderNumber = payload.path("orderNumber").asText();
        String totalPrice  = payload.path("totalPrice").asText();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customerEmail);
        message.setSubject("ご注文確認 - " + orderNumber);
        message.setText("""
            ご注文ありがとうございます。
            注文番号: %s
            合計金額: ¥%s
            """.formatted(orderNumber, totalPrice));

        mailSender.send(message);
        log.info("注文確認メール送信完了: outboxId={}, to={}", event.getId(), customerEmail);
    }
}
