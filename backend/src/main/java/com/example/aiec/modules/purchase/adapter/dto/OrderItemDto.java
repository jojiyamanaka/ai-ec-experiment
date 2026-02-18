package com.example.aiec.modules.purchase.adapter.dto;

import com.example.aiec.modules.product.adapter.dto.ProductDto;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 注文アイテムDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "注文アイテム情報")
public class OrderItemDto {

    @Schema(description = "商品情報")
    private ProductDto product;
    @Schema(description = "数量", example = "2")
    private Integer quantity;
    @Schema(description = "小計（円）", example = "7960")
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
