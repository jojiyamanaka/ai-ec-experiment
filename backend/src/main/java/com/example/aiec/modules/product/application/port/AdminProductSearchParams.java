package com.example.aiec.modules.product.application.port;

import com.example.aiec.modules.product.domain.entity.AllocationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理向け商品検索条件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductSearchParams {
    private String keyword;
    private Long categoryId;
    private Boolean isPublished;
    private Boolean inSalePeriod;
    private AllocationType allocationType;
    private Integer stockThreshold;
    private Boolean zeroStockOnly;
}
