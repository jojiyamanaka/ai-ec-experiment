package com.example.aiec.modules.product.application.port;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.aiec.modules.product.domain.entity.AllocationType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 商品更新リクエスト（管理用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    private String name;

    private String description;

    @Min(value = 1, message = "カテゴリIDは1以上である必要があります")
    private Long categoryId;

    @DecimalMin(value = "0", message = "価格は0以上である必要があります")
    @Digits(integer = 10, fraction = 0, message = "価格は整数である必要があります")
    private BigDecimal price;

    private AllocationType allocationType;

    private Boolean isPublished;

    private Instant publishStartAt;

    private Instant publishEndAt;

    private Instant saleStartAt;

    private Instant saleEndAt;

    private String image;

}
