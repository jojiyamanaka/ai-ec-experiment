package com.example.aiec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品一覧レスポンス
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse {

    private List<ProductDto> items;
    private Long total;
    private Integer page;
    private Integer limit;

}
