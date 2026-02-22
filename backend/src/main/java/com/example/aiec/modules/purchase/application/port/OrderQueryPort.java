package com.example.aiec.modules.purchase.application.port;

import com.example.aiec.modules.purchase.application.port.OrderDto;
import com.example.aiec.modules.purchase.application.port.AdminOrderListResponse;
import com.example.aiec.modules.purchase.application.port.AdminOrderSearchParams;

import java.util.List;

/**
 * 注文クエリAPI（公開インターフェース）
 */
public interface OrderQueryPort {

    /**
     * 注文詳細を取得
     */
    OrderDto getOrderById(Long id, String sessionId, Long userId);

    /**
     * 全注文を取得（管理者用）
     */
    AdminOrderListResponse getAllOrders(AdminOrderSearchParams searchParams, int page, int limit);

    /**
     * 会員の注文履歴を取得
     */
    List<OrderDto> getOrderHistory(Long userId);

}
