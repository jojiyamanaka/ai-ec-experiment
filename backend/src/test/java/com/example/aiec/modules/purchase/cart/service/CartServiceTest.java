package com.example.aiec.modules.purchase.cart.service;

import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.purchase.adapter.dto.AddToCartRequest;
import com.example.aiec.modules.purchase.cart.entity.Cart;
import com.example.aiec.modules.purchase.cart.entity.CartItem;
import com.example.aiec.modules.purchase.cart.repository.CartItemRepository;
import com.example.aiec.modules.purchase.cart.repository.CartRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CartService の業務ルール単体テスト。
 * JPA / Spring コンテキストなし。
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryCommandPort inventoryCommand;

    @InjectMocks
    CartService cartService;

    // ── ヘルパー ─────────────────────────────────────────────────────────────

    private Cart buildCart(String sessionId) {
        Cart cart = new Cart();
        cart.setSessionId(sessionId);
        cart.setCreatedAt(Instant.now());
        cart.setUpdatedAt(Instant.now());
        return cart;
    }

    private Product buildProduct(Long id, boolean published) {
        Product p = new Product();
        p.setId(id);
        p.setName("テスト商品");
        p.setPrice(BigDecimal.valueOf(1000));
        p.setImage("/img/test.jpg");
        p.setDescription("テスト用商品説明");
        p.setStock(10);
        p.setIsPublished(published);
        return p;
    }

    private AddToCartRequest req(Long productId, int quantity) {
        AddToCartRequest r = new AddToCartRequest();
        r.setProductId(productId);
        r.setQuantity(quantity);
        return r;
    }

    // ── addToCart: 商品未存在 ─────────────────────────────────────────────────

    @Test
    void addToCart_productNotFound_shouldThrowResourceNotFoundException() {
        when(cartRepository.findBySessionId("sess")).thenReturn(Optional.of(buildCart("sess")));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> cartService.addToCart("sess", req(1L, 1)));
    }

    // ── addToCart: 非公開商品 ─────────────────────────────────────────────────

    @Test
    void addToCart_productNotPublished_shouldThrowBusinessException() {
        when(cartRepository.findBySessionId("sess")).thenReturn(Optional.of(buildCart("sess")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, false)));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> cartService.addToCart("sess", req(1L, 1)))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("ITEM_NOT_AVAILABLE"));
    }

    // ── addToCart: 最大数量超過 ───────────────────────────────────────────────

    @Test
    void addToCart_exceedsMaxQuantity_shouldThrowBusinessException() {
        Cart cart = buildCart("sess");
        Product product = buildProduct(1L, true);
        CartItem existing = new CartItem();
        existing.setProduct(product);
        existing.setQuantity(8); // 既に8個 + 2個追加 = 10 > 9

        when(cartRepository.findBySessionId("sess")).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartAndProduct(cart, product)).thenReturn(Optional.of(existing));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> cartService.addToCart("sess", req(1L, 2)))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("INVALID_QUANTITY"));
    }

    // ── addToCart: 正常追加（新規アイテム） ──────────────────────────────────

    @Test
    void addToCart_newItem_shouldCreateReservationAndSaveItem() {
        Cart cart = buildCart("sess");
        Product product = buildProduct(1L, true);

        when(cartRepository.findBySessionId("sess")).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartAndProduct(cart, product)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        cartService.addToCart("sess", req(1L, 2));

        verify(inventoryCommand).createReservation("sess", 1L, 2);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    // ── removeFromCart: 商品未存在 ────────────────────────────────────────────

    @Test
    void removeFromCart_productNotFound_shouldThrowResourceNotFoundException() {
        when(cartRepository.findBySessionId("sess")).thenReturn(Optional.of(buildCart("sess")));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> cartService.removeFromCart("sess", 99L));
    }

    // ── removeFromCart: 正常削除 ──────────────────────────────────────────────

    @Test
    void removeFromCart_existingItem_shouldReleaseReservationAndDeleteItem() {
        Cart cart = buildCart("sess");
        Product product = buildProduct(1L, true);
        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cart.addItem(cartItem);

        when(cartRepository.findBySessionId("sess")).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartAndProduct(cart, product)).thenReturn(Optional.of(cartItem));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        cartService.removeFromCart("sess", 1L);

        verify(inventoryCommand).releaseReservation("sess", 1L);
        verify(cartItemRepository).delete(cartItem);
    }

}
