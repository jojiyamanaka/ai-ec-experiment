package com.example.aiec.service;

import com.example.aiec.dto.ProductDto;
import com.example.aiec.dto.ProductListResponse;
import com.example.aiec.dto.UpdateProductRequest;
import com.example.aiec.entity.Product;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品サービス
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 商品一覧を取得（公開されている商品のみ）
     */
    @Transactional(readOnly = true)
    public ProductListResponse getPublishedProducts(int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Product> productPage = productRepository.findByIsPublishedTrue(pageable);

        List<ProductDto> items = productPage.getContent().stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());

        return new ProductListResponse(
                items,
                productPage.getTotalElements(),
                page,
                limit
        );
    }

    /**
     * 商品詳細を取得（公開されている商品のみ）
     */
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        // 非公開商品は取得できない（セキュリティ対策）
        if (!product.getIsPublished()) {
            throw new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません");
        }

        return ProductDto.fromEntity(product);
    }

    /**
     * 商品を更新（管理用）
     */
    @Transactional
    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        // 更新可能なフィールドのみ更新
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getIsPublished() != null) {
            product.setIsPublished(request.getIsPublished());
        }

        Product updated = productRepository.save(product);
        return ProductDto.fromEntity(updated);
    }

}
