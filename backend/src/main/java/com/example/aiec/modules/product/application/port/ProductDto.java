package com.example.aiec.modules.product.application.port;

import com.example.aiec.modules.product.domain.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 商品DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品情報")
public class ProductDto {

    @Schema(description = "商品ID", example = "1")
    private Long id;
    @Schema(description = "商品名", example = "AIスピーカー")
    private String name;
    @Schema(description = "税込価格（円）", example = "3980")
    private BigDecimal price;
    @Schema(description = "商品画像URL", example = "/images/speaker.jpg")
    private String image;
    @Schema(description = "商品説明")
    private String description;
    @Schema(description = "在庫数", example = "100")
    private Integer stock;
    @Schema(description = "公開状態", example = "true")
    private Boolean isPublished;

    /**
     * エンティティから DTO を生成
     */
    public static ProductDto fromEntity(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImage(),
                product.getDescription(),
                product.getStock(),
                product.getIsPublished()
        );
    }

}
