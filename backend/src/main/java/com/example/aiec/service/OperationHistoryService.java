package com.example.aiec.service;

import com.example.aiec.entity.OperationHistory;
import com.example.aiec.entity.User;
import com.example.aiec.repository.OperationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作履歴サービス
 */
@Service
@RequiredArgsConstructor
public class OperationHistoryService {

    private final OperationHistoryRepository operationHistoryRepository;

    /**
     * ログイン成功を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginSuccess(User user) {
        OperationHistory log = new OperationHistory();
        log.setEventType("LOGIN_SUCCESS");
        log.setDetails("User logged in successfully");
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        operationHistoryRepository.save(log);
    }

    /**
     * ログイン失敗を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginFailure(String email) {
        OperationHistory log = new OperationHistory();
        log.setEventType("LOGIN_FAILURE");
        log.setDetails("Login attempt failed");
        log.setUserEmail(email);
        operationHistoryRepository.save(log);
    }

    /**
     * 認可エラーを記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthorizationError(User user, String requestPath) {
        OperationHistory log = new OperationHistory();
        log.setEventType("AUTHORIZATION_ERROR");
        log.setDetails("User attempted to access admin resource without permission");
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        log.setRequestPath(requestPath);
        operationHistoryRepository.save(log);
    }

    /**
     * 管理操作を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminAction(User user, String requestPath, String details) {
        OperationHistory log = new OperationHistory();
        log.setEventType("ADMIN_ACTION");
        log.setDetails(details);
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        log.setRequestPath(requestPath);
        operationHistoryRepository.save(log);
    }
}
