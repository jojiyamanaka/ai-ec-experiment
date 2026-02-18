package com.example.aiec.modules.shared.application.usecase;

import com.example.aiec.modules.shared.domain.entity.OperationHistory;
import com.example.aiec.modules.shared.domain.repository.OperationHistoryRepository;
import com.example.aiec.modules.shared.event.OperationPerformedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 監査ログイベントハンドラ
 *
 * REQUIRES_NEW で別トランザクションとして実行されるため、
 * メイン処理が失敗してもログは記録される
 */
@Service
@RequiredArgsConstructor
@Slf4j
class OperationHistoryEventHandler {

    private final OperationHistoryRepository operationHistoryRepository;

    /**
     * 操作実行イベントを受信して監査ログに記録
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleOperationPerformed(OperationPerformedEvent event) {
        try {
            OperationHistory history = new OperationHistory();
            history.setOperationType(event.operationType());
            history.setPerformedBy(event.performedBy());
            history.setRequestPath(event.requestPath());
            history.setDetails(event.details());

            operationHistoryRepository.save(history);
            log.debug("監査ログを記録しました: operationType={}, performedBy={}",
                    event.operationType(), event.performedBy());
        } catch (Exception e) {
            log.error("監査ログの記録に失敗しました: event={}", event, e);
            // 監査ログ記録失敗はメイン処理に影響させない（ログ出力のみ）
        }
    }

}
