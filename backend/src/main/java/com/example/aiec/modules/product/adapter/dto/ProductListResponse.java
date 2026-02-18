package com.example.aiec.modules.product.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 商品一覧レスポンス
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品一覧レスポンス")
public class ProductListResponse {

    @Schema(description = "商品リスト")
    private List<ProductDto> items;
    @Schema(description = "総件数", example = "50")
    private Long total;
    @Schema(description = "現在のページ番号", example = "1")
    private Integer page;
    @Schema(description = "1ページあたりの件数", example = "20")
    private Integer limit;

}
