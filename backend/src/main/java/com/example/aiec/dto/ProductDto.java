package com.example.aiec.dto;

import com.example.aiec.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long id;
    private String name;
    private Integer price;
    private String image;
    private String description;
    private Integer stock;
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
