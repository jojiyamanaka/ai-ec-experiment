package com.example.aiec.modules.product.application.port;

import com.example.aiec.modules.product.domain.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

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
    @Schema(description = "商品コード", example = "P000001")
    private String productCode;
    @Schema(description = "カテゴリID", example = "1")
    private Long categoryId;
    @Schema(description = "カテゴリ名", example = "未分類")
    private String categoryName;
    @Schema(description = "公開開始日時", example = "2026-02-20T00:00:00Z")
    private Instant publishStartAt;
    @Schema(description = "公開終了日時", example = "2026-12-31T23:59:59Z")
    private Instant publishEndAt;
    @Schema(description = "販売開始日時", example = "2026-02-20T00:00:00Z")
    private Instant saleStartAt;
    @Schema(description = "販売終了日時", example = "2026-12-31T23:59:59Z")
    private Instant saleEndAt;

    /**
     * エンティティから DTO を生成
     */
    public static ProductDto fromEntity(Product product) {
        return fromEntity(product, null);
    }

    public static ProductDto fromEntity(Product product, String categoryName) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImage(),
                product.getDescription(),
                product.getStock(),
                product.getIsPublished(),
                product.getProductCode(),
                product.getCategoryId(),
                categoryName,
                product.getPublishStartAt(),
                product.getPublishEndAt(),
                product.getSaleStartAt(),
                product.getSaleEndAt()
        );
    }

}
