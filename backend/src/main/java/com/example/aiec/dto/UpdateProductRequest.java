package com.example.aiec.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品更新リクエスト（管理用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @DecimalMin(value = "0", message = "価格は0以上である必要があります")
    private BigDecimal price;

    @Min(value = 0, message = "在庫数は0以上である必要があります")
    private Integer stock;

    private Boolean isPublished;

}
