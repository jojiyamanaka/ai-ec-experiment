package com.example.aiec.modules.purchase.application.port;

import com.example.aiec.modules.purchase.application.port.OrderDto;

/**
 * 注文コマンドAPI（公開インターフェース）
 */
public interface OrderCommandPort {

    /**
     * 注文を作成
     */
    OrderDto createOrder(String sessionId, String cartId, Long userId);

    /**
     * 注文をキャンセル
     */
    OrderDto cancelOrder(Long orderId, String sessionId, Long userId);

    /**
     * 注文を確認（PENDING → CONFIRMED）
     */
    OrderDto confirmOrder(Long orderId);

    /**
     * 注文を発送完了（PREPARING_SHIPMENT → SHIPPED）
     */
    OrderDto markShipped(Long orderId);

    /**
     * 注文を配達完了（SHIPPED → DELIVERED）
     */
    OrderDto deliverOrder(Long orderId);

    /**
     * 本引当を再試行
     */
    OrderDto retryAllocation(Long orderId);

}
