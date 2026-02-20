package com.example.aiec.modules.product.application.usecase;

import com.example.aiec.modules.product.application.port.CreateProductRequest;
import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.product.application.port.ProductListResponse;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.entity.ProductCategory;
import com.example.aiec.modules.product.domain.repository.ProductCategoryRepository;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductUseCaseTest {

    @Mock ProductRepository productRepository;
    @Mock ProductCategoryRepository productCategoryRepository;

    @InjectMocks
    ProductUseCase productUseCase;

    @Test
    void createProduct_invalidSchedule_shouldThrowBusinessException() {
        CreateProductRequest request = validCreateRequest();
        request.setPublishStartAt(Instant.parse("2026-02-21T00:00:00Z"));
        request.setPublishEndAt(Instant.parse("2026-02-20T00:00:00Z"));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> productUseCase.createProduct(request))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("INVALID_SCHEDULE"));
    }

    @Test
    void createProduct_inactiveCategory_shouldThrowBusinessException() {
        CreateProductRequest request = validCreateRequest();
        ProductCategory inactiveCategory = category(10L, "非公開カテゴリ", false);

        when(productRepository.existsByProductCode("P000123")).thenReturn(false);
        when(productCategoryRepository.findById(10L)).thenReturn(Optional.of(inactiveCategory));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> productUseCase.createProduct(request))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("CATEGORY_INACTIVE"));
    }

    @Test
    void getProduct_notVisible_shouldThrowResourceNotFoundException() {
        when(productRepository.findVisibleById(eq(1L), any(Instant.class))).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> productUseCase.getProduct(1L))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("ITEM_NOT_FOUND"));
    }

    @Test
    void getPublishedProducts_shouldMapCategoryName() {
        Product product = product(1L, "P000001", 10L, true);
        ProductCategory category = category(10L, "ガジェット", true);

        when(productRepository.findPublishedForCustomer(any(Instant.class), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));
        when(productCategoryRepository.findAllById(any())).thenReturn(List.of(category));

        ProductListResponse response = productUseCase.getPublishedProducts(1, 20);

        assertThat(response.getItems()).hasSize(1);
        ProductDto dto = response.getItems().get(0);
        assertThat(dto.getProductCode()).isEqualTo("P000001");
        assertThat(dto.getCategoryName()).isEqualTo("ガジェット");
    }

    private CreateProductRequest validCreateRequest() {
        return new CreateProductRequest(
                "P000123",
                "新商品",
                "説明",
                10L,
                BigDecimal.valueOf(1000),
                10,
                true,
                Instant.parse("2026-02-20T00:00:00Z"),
                Instant.parse("2026-02-28T23:59:59Z"),
                Instant.parse("2026-02-20T00:00:00Z"),
                Instant.parse("2026-02-27T23:59:59Z"),
                "/images/item.jpg"
        );
    }

    private ProductCategory category(Long id, String name, boolean published) {
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setName(name);
        category.setDisplayOrder(0);
        category.setIsPublished(published);
        return category;
    }

    private Product product(Long id, String productCode, Long categoryId, boolean published) {
        Product product = new Product();
        product.setId(id);
        product.setProductCode(productCode);
        product.setName("商品");
        product.setPrice(BigDecimal.valueOf(1000));
        product.setImage("/images/item.jpg");
        product.setDescription("説明");
        product.setStock(10);
        product.setCategoryId(categoryId);
        product.setIsPublished(published);
        return product;
    }
}
