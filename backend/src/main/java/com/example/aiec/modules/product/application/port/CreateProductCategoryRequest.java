package com.example.aiec.modules.product.application.port;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductCategoryRequest {

    @NotBlank(message = "カテゴリ名は必須です")
    private String name;

    @Min(value = 0, message = "表示順は0以上である必要があります")
    private Integer displayOrder;

    private Boolean isPublished;
}
