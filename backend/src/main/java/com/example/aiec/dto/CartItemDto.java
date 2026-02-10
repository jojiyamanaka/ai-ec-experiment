package com.example.aiec.dto;

import com.example.aiec.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * カートアイテムDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {

    private Long id;
    private ProductDto product;
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
