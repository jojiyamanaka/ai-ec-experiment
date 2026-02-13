package com.example.aiec.service;

import com.example.aiec.entity.BoUser;
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
        log.setOperationType("LOGIN_SUCCESS");
        log.setDetails("User logged in successfully");
        log.setPerformedBy(user.getEmail());
        operationHistoryRepository.save(log);
    }

    /**
     * BoUser のログイン成功を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginSuccess(BoUser boUser) {
        OperationHistory log = new OperationHistory();
        log.setOperationType("LOGIN_SUCCESS");
        log.setDetails("BoUser login successful: " + boUser.getEmail());
        log.setPerformedBy(boUser.getEmail());
        log.setRequestPath("/api/bo-auth/login");
        operationHistoryRepository.save(log);
    }

    /**
     * ログイン失敗を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginFailure(String email) {
        OperationHistory log = new OperationHistory();
        log.setOperationType("LOGIN_FAILURE");
        log.setDetails("Login attempt failed");
        log.setPerformedBy(email);
        operationHistoryRepository.save(log);
    }

    /**
     * 認可エラーを記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthorizationError(User user, String requestPath) {
        OperationHistory log = new OperationHistory();
        log.setOperationType("AUTHORIZATION_ERROR");
        log.setDetails("User attempted to access admin resource without permission");
        log.setPerformedBy(user.getEmail());
        log.setRequestPath(requestPath);
        operationHistoryRepository.save(log);
    }

    /**
     * BoUser の認可エラーを記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthorizationError(BoUser boUser, String requestPath) {
        OperationHistory log = new OperationHistory();
        log.setOperationType("AUTHORIZATION_ERROR");
        log.setDetails("BoUser attempted to access admin resource without permission");
        log.setPerformedBy(boUser.getEmail());
        log.setRequestPath(requestPath);
        operationHistoryRepository.save(log);
    }

    /**
     * 管理操作を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminAction(User user, String requestPath, String details) {
        OperationHistory log = new OperationHistory();
        log.setOperationType("ADMIN_ACTION");
        log.setDetails(details);
        log.setPerformedBy(user.getEmail());
        log.setRequestPath(requestPath);
        operationHistoryRepository.save(log);
    }

    /**
     * BoUser の管理操作を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminAction(BoUser boUser, String requestPath, String details) {
        OperationHistory log = new OperationHistory();
        log.setOperationType("ADMIN_ACTION");
        log.setDetails(details);
        log.setPerformedBy(boUser.getEmail());
        log.setRequestPath(requestPath);
        operationHistoryRepository.save(log);
    }
}
