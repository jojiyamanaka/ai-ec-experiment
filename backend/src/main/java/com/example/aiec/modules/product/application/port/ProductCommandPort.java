package com.example.aiec.modules.product.application.port;

import com.example.aiec.modules.product.adapter.dto.ProductDto;
import com.example.aiec.modules.product.adapter.dto.UpdateProductRequest;

/**
 * 商品コマンドAPI（公開インターフェース）
 */
public interface ProductCommandPort {

    /**
     * 商品を更新（管理用）
     */
    ProductDto updateProduct(Long id, UpdateProductRequest request);

}
