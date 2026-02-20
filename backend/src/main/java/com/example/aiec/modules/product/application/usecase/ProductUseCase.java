package com.example.aiec.modules.product.application.usecase;

import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.product.application.port.ProductListResponse;
import com.example.aiec.modules.product.application.port.CreateProductRequest;
import com.example.aiec.modules.product.application.port.CreateProductCategoryRequest;
import com.example.aiec.modules.product.application.port.ProductCategoryDto;
import com.example.aiec.modules.product.application.port.UpdateProductRequest;
import com.example.aiec.modules.product.application.port.UpdateProductCategoryRequest;
import com.example.aiec.modules.product.application.port.ProductCommandPort;
import com.example.aiec.modules.product.application.port.ProductQueryPort;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.entity.ProductCategory;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.product.domain.repository.ProductCategoryRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ConflictException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品ユースケース（Port実装）
 */
@Service
@RequiredArgsConstructor
class ProductUseCase implements ProductQueryPort, ProductCommandPort {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    private static final String DEFAULT_IMAGE = "/images/no-image.png";

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProductListResponse getPublishedProducts(int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(safePage - 1, safeLimit);
        Instant now = Instant.now();
        Page<Product> productPage = productRepository.findPublishedForCustomer(now, pageable);
        Map<Long, String> categoryNames = loadCategoryNames(productPage.getContent());

        List<ProductDto> items = productPage.getContent().stream()
                .map(product -> ProductDto.fromEntity(product, categoryNames.get(product.getCategoryId())))
                .collect(Collectors.toList());

        return new ProductListResponse(
                items,
                productPage.getTotalElements(),
                safePage,
                safeLimit
        );
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProductDto getProduct(Long id) {
        Instant now = Instant.now();
        Product product = productRepository.findVisibleById(id, now)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));
        String categoryName = loadCategoryName(product.getCategoryId());
        return ProductDto.fromEntity(product, categoryName);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProductListResponse getAdminProducts(int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(safePage - 1, safeLimit);
        Page<Product> productPage = productRepository.findAll(pageable);
        Map<Long, String> categoryNames = loadCategoryNames(productPage.getContent());
        List<ProductDto> items = productPage.getContent().stream()
                .map(product -> ProductDto.fromEntity(product, categoryNames.get(product.getCategoryId())))
                .collect(Collectors.toList());
        return new ProductListResponse(items, productPage.getTotalElements(), safePage, safeLimit);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProductDto getAdminProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));
        String categoryName = loadCategoryName(product.getCategoryId());
        return ProductDto.fromEntity(product, categoryName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDto createProduct(CreateProductRequest request) {
        validateSchedule(
                request.getPublishStartAt(),
                request.getPublishEndAt(),
                request.getSaleStartAt(),
                request.getSaleEndAt()
        );

        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new ConflictException("PRODUCT_CODE_ALREADY_EXISTS", "この品番は既に登録されています");
        }

        ProductCategory category = validateCategoryForAssignment(request.getCategoryId());

        Product product = new Product();
        product.setProductCode(request.getProductCode().trim());
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategoryId(category.getId());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : true);
        product.setPublishStartAt(request.getPublishStartAt());
        product.setPublishEndAt(request.getPublishEndAt());
        product.setSaleStartAt(request.getSaleStartAt());
        product.setSaleEndAt(request.getSaleEndAt());
        product.setImage(request.getImage() != null && !request.getImage().isBlank() ? request.getImage() : DEFAULT_IMAGE);

        Product created = productRepository.save(product);
        return ProductDto.fromEntity(created, category.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));
        ProductCategory category = null;

        // 更新可能なフィールドのみ更新
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            category = validateCategoryForAssignment(request.getCategoryId());
            product.setCategoryId(category.getId());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getIsPublished() != null) {
            product.setIsPublished(request.getIsPublished());
        }
        if (request.getImage() != null) {
            product.setImage(request.getImage());
        }
        if (request.getPublishStartAt() != null) {
            product.setPublishStartAt(request.getPublishStartAt());
        }
        if (request.getPublishEndAt() != null) {
            product.setPublishEndAt(request.getPublishEndAt());
        }
        if (request.getSaleStartAt() != null) {
            product.setSaleStartAt(request.getSaleStartAt());
        }
        if (request.getSaleEndAt() != null) {
            product.setSaleEndAt(request.getSaleEndAt());
        }

        validateSchedule(
                product.getPublishStartAt(),
                product.getPublishEndAt(),
                product.getSaleStartAt(),
                product.getSaleEndAt()
        );

        Product updated = productRepository.save(product);
        String categoryName = category != null ? category.getName() : loadCategoryName(updated.getCategoryId());
        return ProductDto.fromEntity(updated, categoryName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductCategoryDto createCategory(CreateProductCategoryRequest request) {
        String normalizedName = request.getName().trim();
        if (productCategoryRepository.existsByName(normalizedName)) {
            throw new ConflictException("CATEGORY_ALREADY_EXISTS", "このカテゴリ名は既に登録されています");
        }

        ProductCategory category = new ProductCategory();
        category.setName(normalizedName);
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : true);
        ProductCategory created = productCategoryRepository.save(category);
        return ProductCategoryDto.fromEntity(created);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductCategoryDto updateCategory(Long id, UpdateProductCategoryRequest request) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "カテゴリが見つかりません"));

        if (request.getName() != null) {
            String normalizedName = request.getName().trim();
            productCategoryRepository.findByName(normalizedName)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ConflictException("CATEGORY_ALREADY_EXISTS", "このカテゴリ名は既に登録されています");
                    });
            category.setName(normalizedName);
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsPublished() != null) {
            category.setIsPublished(request.getIsPublished());
        }
        ProductCategory updated = productCategoryRepository.save(category);
        return ProductCategoryDto.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ProductCategoryDto> getCategories() {
        return productCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(ProductCategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    private ProductCategory validateCategoryForAssignment(Long categoryId) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "カテゴリが見つかりません"));
        if (!category.getIsPublished()) {
            throw new BusinessException("CATEGORY_INACTIVE", "非公開カテゴリは指定できません");
        }
        return category;
    }

    private void validateSchedule(
            Instant publishStartAt,
            Instant publishEndAt,
            Instant saleStartAt,
            Instant saleEndAt
    ) {
        if (publishStartAt != null && publishEndAt != null && publishStartAt.isAfter(publishEndAt)) {
            throw new BusinessException("INVALID_SCHEDULE", "公開期間の指定が不正です");
        }
        if (saleStartAt != null && saleEndAt != null && saleStartAt.isAfter(saleEndAt)) {
            throw new BusinessException("INVALID_SCHEDULE", "販売期間の指定が不正です");
        }
        if (saleStartAt != null && publishStartAt != null && saleStartAt.isBefore(publishStartAt)) {
            throw new BusinessException("INVALID_SCHEDULE", "販売期間は公開期間内で指定してください");
        }
        if (saleEndAt != null && publishEndAt != null && saleEndAt.isAfter(publishEndAt)) {
            throw new BusinessException("INVALID_SCHEDULE", "販売期間は公開期間内で指定してください");
        }
    }

    private Map<Long, String> loadCategoryNames(List<Product> products) {
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return productCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
    }

    private String loadCategoryName(Long categoryId) {
        return productCategoryRepository.findById(categoryId)
                .map(ProductCategory::getName)
                .orElse("");
    }

}
