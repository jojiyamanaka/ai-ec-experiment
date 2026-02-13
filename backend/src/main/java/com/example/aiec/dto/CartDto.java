package com.example.aiec.dto;

import com.example.aiec.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * カートDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {

    private List<CartItemDto> items;
    private Integer totalQuantity;
    private BigDecimal totalPrice;

    /**
     * エンティティから DTO を生成
     */
    public static CartDto fromEntity(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(CartItemDto::fromEntity)
                .collect(Collectors.toList());

        return new CartDto(
                items,
                cart.getTotalQuantity(),
                cart.getTotalPrice()
        );
    }

}
