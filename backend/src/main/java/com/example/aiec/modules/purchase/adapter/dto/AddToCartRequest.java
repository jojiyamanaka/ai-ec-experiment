package com.example.aiec.modules.purchase.adapter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * カート追加リクエスト
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {

    @NotNull(message = "商品IDは必須です")
    private Long productId;

    @Min(value = 1, message = "数量は1以上である必要があります")
    @Max(value = 9, message = "1商品あたりの最大数量は9個です")
    private Integer quantity = 1;

}
