package com.example.aiec.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品更新リクエスト（管理用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Min(value = 0, message = "価格は0以上である必要があります")
    private Integer price;

    @Min(value = 0, message = "在庫数は0以上である必要があります")
    private Integer stock;

    private Boolean isPublished;

}
