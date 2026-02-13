package com.example.aiec.repository;

import com.example.aiec.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 注文リポジトリ
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 注文番号で検索
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * セッションIDで注文一覧を取得
     */
    List<Order> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * 会員IDで注文一覧を取得（作成日時降順）
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 特定ユーザーの注文数をカウント
     */
    Long countByUserId(Long userId);

    /**
     * 特定ユーザーの総購入額を集計
     */
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.user.id = :userId")
    Optional<Long> sumTotalPriceByUserId(@Param("userId") Long userId);

}
