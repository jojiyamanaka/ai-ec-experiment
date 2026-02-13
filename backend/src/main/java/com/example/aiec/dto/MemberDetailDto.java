package com.example.aiec.dto;

import com.example.aiec.entity.Role;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MemberDetailDto {
    private Long id;
    private String email;
    private String displayName;
    private Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private OrderSummary orderSummary;

    @Data
    public static class OrderSummary {
        private Long totalOrders;
        private Long totalAmount;
    }
}
