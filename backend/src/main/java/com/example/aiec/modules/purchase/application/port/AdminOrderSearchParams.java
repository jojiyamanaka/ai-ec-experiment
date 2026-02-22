package com.example.aiec.modules.purchase.application.port;

import com.example.aiec.modules.purchase.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理向け注文検索条件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderSearchParams {
    private String orderNumber;
    private String customerEmail;
    private List<Order.OrderStatus> statuses;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal totalPriceMin;
    private BigDecimal totalPriceMax;
    private Boolean allocationIncomplete;
    private Boolean unshipped;
}
