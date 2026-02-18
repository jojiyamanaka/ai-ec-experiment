package com.example.aiec.modules.shared.domain.repository;

import com.example.aiec.modules.shared.domain.entity.OperationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 操作履歴リポジトリ
 */
public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {
    // 基本的なCRUD操作のみ（検索機能は将来拡張）
}
