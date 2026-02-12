package com.example.aiec.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数量変更リクエスト
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuantityRequest {

    @NotNull(message = "数量は必須です")
    @Min(value = 1, message = "数量は1以上である必要があります")
    @Max(value = 9, message = "1商品あたりの最大数量は9個です")
    private Integer quantity;

}
