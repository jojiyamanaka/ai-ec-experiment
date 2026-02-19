package com.example.aiec.modules.purchase.adapter.dto;

import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.purchase.cart.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * カートアイテムDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "カートアイテム情報")
public class CartItemDto {

    @Schema(description = "商品ID", example = "1")
    private Long id;
    @Schema(description = "商品情報")
    private ProductDto product;
    @Schema(description = "数量", example = "2")
    private Integer quantity;

    /**
     * エンティティから DTO を生成
     */
    public static CartItemDto fromEntity(CartItem cartItem) {
        return new CartItemDto(
                cartItem.getProduct().getId(), // APIではカートアイテムIDは商品IDと同じ
                ProductDto.fromEntity(cartItem.getProduct()),
                cartItem.getQuantity()
        );
    }

}
