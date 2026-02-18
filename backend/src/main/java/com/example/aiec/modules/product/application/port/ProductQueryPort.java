package com.example.aiec.modules.product.application.port;

import com.example.aiec.modules.product.adapter.dto.ProductDto;
import com.example.aiec.modules.product.adapter.dto.ProductListResponse;

/**
 * 商品クエリAPI（公開インターフェース）
 */
public interface ProductQueryPort {

    /**
     * 商品一覧を取得（公開されている商品のみ）
     */
    ProductListResponse getPublishedProducts(int page, int limit);

    /**
     * 商品詳細を取得（公開されている商品のみ）
     */
    ProductDto getProduct(Long id);

}
