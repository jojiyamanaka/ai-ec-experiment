package com.example.aiec.modules.backoffice.adapter.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class MemberDetailDto {
    private Long id;
    private String email;
    private String displayName;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private OrderSummary orderSummary;

    @Data
    public static class OrderSummary {
        private Long totalOrders;
        private BigDecimal totalAmount;
    }
}
