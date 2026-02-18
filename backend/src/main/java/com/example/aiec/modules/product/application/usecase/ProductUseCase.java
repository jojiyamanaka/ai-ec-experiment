package com.example.aiec.modules.product.application.usecase;

import com.example.aiec.modules.product.adapter.dto.ProductDto;
import com.example.aiec.modules.product.adapter.dto.ProductListResponse;
import com.example.aiec.modules.product.adapter.dto.UpdateProductRequest;
import com.example.aiec.modules.product.application.port.ProductCommandPort;
import com.example.aiec.modules.product.application.port.ProductQueryPort;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品ユースケース（Port実装）
 */
@Service
@RequiredArgsConstructor
class ProductUseCase implements ProductQueryPort, ProductCommandPort {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
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

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProductDto getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        // 非公開商品は取得できない（セキュリティ対策）
        if (!product.getIsPublished()) {
            throw new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません");
        }

        return ProductDto.fromEntity(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
