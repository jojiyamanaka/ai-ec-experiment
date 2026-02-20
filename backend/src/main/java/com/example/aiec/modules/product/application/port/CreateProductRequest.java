package com.example.aiec.modules.product.application.port;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.aiec.modules.product.domain.entity.AllocationType;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "商品コードは必須です")
    private String productCode;

    @NotBlank(message = "商品名は必須です")
    private String name;

    private String description;

    @NotNull(message = "カテゴリIDは必須です")
    @Min(value = 1, message = "カテゴリIDは1以上である必要があります")
    private Long categoryId;

    @NotNull(message = "価格は必須です")
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
