package com.example.aiec.modules.purchase.adapter.dto;

import com.example.aiec.modules.purchase.cart.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * カートDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "カート情報")
public class CartDto {

    @Schema(description = "カート内商品リスト")
    private List<CartItemDto> items;
    @Schema(description = "合計数量", example = "3")
    private Integer totalQuantity;
    @Schema(description = "合計金額（円）", example = "11940")
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
