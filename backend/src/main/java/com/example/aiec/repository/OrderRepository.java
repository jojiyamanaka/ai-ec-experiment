package com.example.aiec.repository;

import com.example.aiec.entity.ActorType;
import com.example.aiec.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
     * 注文番号採番用シーケンスの次値を取得
     */
    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    Long getNextOrderNumberSequence();

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
    Optional<BigDecimal> sumTotalPriceByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Order o SET o.isDeleted = TRUE, o.deletedAt = CURRENT_TIMESTAMP, o.deletedByType = :deletedByType, o.deletedById = :deletedById WHERE o.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);

}
