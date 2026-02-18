package com.example.aiec.modules.inventory.application.port;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.inventory.adapter.dto.ReservationDto;
import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;
import com.example.aiec.modules.purchase.order.entity.Order;

/**
 * 在庫コマンドAPI（公開インターフェース）
 */
public interface InventoryCommandPort {

    /**
     * 仮引当を作成する
     */
    ReservationDto createReservation(String sessionId, Long productId, Integer quantity);

    /**
     * 仮引当を更新する（カート数量変更時）
     */
    ReservationDto updateReservation(String sessionId, Long productId, Integer newQuantity);

    /**
     * 仮引当を解除する（カートから商品削除時）
     */
    void releaseReservation(String sessionId, Long productId);

    /**
     * セッションの全仮引当を解除する（カートクリア時）
     */
    void releaseAllReservations(String sessionId);

    /**
     * 仮引当を本引当に変換する（注文確定時）
     */
    void commitReservations(String sessionId, Order order);

    /**
     * 本引当を解除する（注文キャンセル時）
     */
    void releaseCommittedReservations(Long orderId);

    /**
     * 在庫調整（差分方式）
     */
    InventoryAdjustment adjustStock(Long productId, Integer quantityDelta, String reason, BoUser admin);

}
