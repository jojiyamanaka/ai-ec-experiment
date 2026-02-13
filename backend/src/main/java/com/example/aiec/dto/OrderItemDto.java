package com.example.aiec.dto;

import com.example.aiec.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 注文アイテムDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    private ProductDto product;
    private Integer quantity;
    private BigDecimal subtotal;

    /**
     * エンティティから DTO を生成
     */
    public static OrderItemDto fromEntity(OrderItem orderItem) {
        return new OrderItemDto(
                ProductDto.fromEntity(orderItem.getProduct()),
                orderItem.getQuantity(),
                orderItem.getSubtotal()
        );
    }

}
