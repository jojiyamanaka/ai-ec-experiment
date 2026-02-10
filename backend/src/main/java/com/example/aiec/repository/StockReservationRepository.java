package com.example.aiec.repository;

import com.example.aiec.entity.StockReservation;
import com.example.aiec.entity.StockReservation.ReservationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 在庫引当リポジトリ
 */
@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    /**
     * 有効在庫を計算する
     * stock - 有効な仮引当の合計 - 本引当の合計
     */
    @Query("SELECT p.stock " +
            "- COALESCE(SUM(CASE WHEN r.type = 'TENTATIVE' AND r.expiresAt > :now THEN r.quantity ELSE 0 END), 0) " +
            "- COALESCE(SUM(CASE WHEN r.type = 'COMMITTED' THEN r.quantity ELSE 0 END), 0) " +
            "FROM Product p LEFT JOIN StockReservation r ON p.id = r.product.id " +
            "WHERE p.id = :productId")
    Integer calculateAvailableStock(@Param("productId") Long productId, @Param("now") LocalDateTime now);

    /**
     * 仮引当の合計数量を取得
     */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM StockReservation r " +
            "WHERE r.product.id = :productId AND r.type = 'TENTATIVE' AND r.expiresAt > :now")
    Integer sumTentativeReserved(@Param("productId") Long productId, @Param("now") LocalDateTime now);

    /**
     * 本引当の合計数量を取得
     */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM StockReservation r " +
            "WHERE r.product.id = :productId AND r.type = 'COMMITTED'")
    Integer sumCommittedReserved(@Param("productId") Long productId);

    /**
     * セッション×商品の有効な仮引当を検索
     */
    @Query("SELECT r FROM StockReservation r " +
            "WHERE r.sessionId = :sessionId AND r.product.id = :productId " +
            "AND r.type = 'TENTATIVE' AND r.expiresAt > :now")
    Optional<StockReservation> findActiveTentative(
            @Param("sessionId") String sessionId,
            @Param("productId") Long productId,
            @Param("now") LocalDateTime now);

    /**
     * セッションの全有効仮引当を取得
     */
    @Query("SELECT r FROM StockReservation r " +
            "WHERE r.sessionId = :sessionId AND r.type = 'TENTATIVE' AND r.expiresAt > :now")
    List<StockReservation> findAllActiveTentativeBySession(
            @Param("sessionId") String sessionId,
            @Param("now") LocalDateTime now);

    /**
     * 注文IDに紐づく本引当を取得
     */
    List<StockReservation> findByOrderIdAndType(Long orderId, ReservationType type);

    /**
     * 期限切れの仮引当を削除
     */
    void deleteByTypeAndExpiresAtBefore(ReservationType type, LocalDateTime now);

}
