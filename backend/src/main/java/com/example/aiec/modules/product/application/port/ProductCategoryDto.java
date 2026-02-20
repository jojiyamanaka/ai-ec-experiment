package com.example.aiec.modules.product.application.port;

import com.example.aiec.modules.product.domain.entity.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品カテゴリ情報")
public class ProductCategoryDto {

    @Schema(description = "カテゴリID", example = "1")
    private Long id;
    @Schema(description = "カテゴリ名", example = "未分類")
    private String name;
    @Schema(description = "表示順", example = "0")
    private Integer displayOrder;
    @Schema(description = "公開状態", example = "true")
    private Boolean isPublished;

    public static ProductCategoryDto fromEntity(ProductCategory category) {
        return new ProductCategoryDto(
                category.getId(),
                category.getName(),
                category.getDisplayOrder(),
                category.getIsPublished()
        );
    }
}
