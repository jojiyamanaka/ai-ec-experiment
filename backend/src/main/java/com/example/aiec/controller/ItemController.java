package com.example.aiec.controller;

import com.example.aiec.dto.ApiResponse;
import com.example.aiec.dto.ProductDto;
import com.example.aiec.dto.ProductListResponse;
import com.example.aiec.dto.UpdateProductRequest;
import com.example.aiec.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品コントローラ
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ProductService productService;

    /**
     * 商品一覧取得
     * GET /api/item
     */
    @GetMapping
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        ProductListResponse response = productService.getPublishedProducts(page, limit);
        return ApiResponse.success(response);
    }

    /**
     * 商品詳細取得
     * GET /api/item/:id
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductDto> getProduct(@PathVariable Long id) {
        ProductDto product = productService.getProductById(id);
        return ApiResponse.success(product);
    }

    /**
     * 商品更新（管理用）
     * PUT /api/item/:id
     */
    @PutMapping("/{id}")
    public ApiResponse<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductDto product = productService.updateProduct(id, request);
        return ApiResponse.success(product);
    }

}
