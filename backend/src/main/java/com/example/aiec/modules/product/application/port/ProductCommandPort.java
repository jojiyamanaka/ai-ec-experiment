package com.example.aiec.modules.product.application.port;

import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.product.application.port.UpdateProductRequest;

/**
 * 商品コマンドAPI（公開インターフェース）
 */
public interface ProductCommandPort {

    /**
     * 商品を更新（管理用）
     */
    ProductDto updateProduct(Long id, UpdateProductRequest request);

}
