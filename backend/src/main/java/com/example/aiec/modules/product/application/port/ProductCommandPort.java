package com.example.aiec.modules.product.application.port;

import java.util.List;

/**
 * 商品コマンドAPI（公開インターフェース）
 */
public interface ProductCommandPort {

    ProductDto createProduct(CreateProductRequest request);

    /**
     * 商品を更新（管理用）
     */
    ProductDto updateProduct(Long id, UpdateProductRequest request);

    ProductCategoryDto createCategory(CreateProductCategoryRequest request);

    ProductCategoryDto updateCategory(Long id, UpdateProductCategoryRequest request);

    List<ProductCategoryDto> getCategories();

}
