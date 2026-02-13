package com.example.aiec.service;

import com.example.aiec.dto.AddToCartRequest;
import com.example.aiec.dto.CartDto;
import com.example.aiec.entity.Cart;
import com.example.aiec.entity.CartItem;
import com.example.aiec.entity.Product;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.CartItemRepository;
import com.example.aiec.repository.CartRepository;
import com.example.aiec.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * カートサービス
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_QUANTITY_PER_ITEM = 9;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    /**
     * カートを取得（存在しない場合は作成）
     */
    @Transactional(rollbackFor = Exception.class)
    public CartDto getOrCreateCart(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> createCart(sessionId));

        // 非公開商品をカートから除外
        List<CartItem> unpublishedItems = cart.getItems().stream()
                .filter(item -> !item.getProduct().getIsPublished())
                .toList();

        if (!unpublishedItems.isEmpty()) {
            for (CartItem item : unpublishedItems) {
                inventoryService.releaseReservation(sessionId, item.getProduct().getId());
                cart.removeItem(item);
                cartItemRepository.delete(item);
            }
            cart.setUpdatedAt(Instant.now());
            cartRepository.save(cart);
        }

        return CartDto.fromEntity(cart);
    }

    /**
     * カートに商品を追加
     */
    @Transactional(rollbackFor = Exception.class)
    public CartDto addToCart(String sessionId, AddToCartRequest request) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> createCart(sessionId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        if (!product.getIsPublished()) {
            throw new BusinessException("ITEM_NOT_AVAILABLE", "この商品は現在購入できません");
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);
        int currentQuantity = existingItem.map(CartItem::getQuantity).orElse(0);
        int totalQuantity = currentQuantity + request.getQuantity();
        if (totalQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new BusinessException("INVALID_QUANTITY",
                    "1商品あたりの最大数量は" + MAX_QUANTITY_PER_ITEM + "個です（現在カートに" + currentQuantity + "個あります）");
        }

        // 仮引当を作成（有効在庫チェック込み）
        inventoryService.createReservation(sessionId, request.getProductId(), request.getQuantity());

        // 既存のカートアイテムを検索
        if (existingItem.isPresent()) {
            // 既存のアイテムがある場合は数量を追加
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // 新しいアイテムを追加
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return CartDto.fromEntity(cart);
    }

    /**
     * カート内商品の数量を変更
     */
    @Transactional(rollbackFor = Exception.class)
    public CartDto updateCartItemQuantity(String sessionId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CART_NOT_FOUND", "カートが見つかりません"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "カート内に商品が見つかりません"));

        // 仮引当を更新（有効在庫チェック込み）
        inventoryService.updateReservation(sessionId, productId, quantity);

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return CartDto.fromEntity(cart);
    }

    /**
     * カートから商品を削除
     */
    @Transactional(rollbackFor = Exception.class)
    public CartDto removeFromCart(String sessionId, Long productId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CART_NOT_FOUND", "カートが見つかりません"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "カート内に商品が見つかりません"));

        // 仮引当を解除
        inventoryService.releaseReservation(sessionId, productId);

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return CartDto.fromEntity(cart);
    }

    /**
     * カートをクリア
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(String sessionId) {
        // 全仮引当を解除
        inventoryService.releaseAllReservations(sessionId);

        cartRepository.findBySessionId(sessionId)
                .ifPresent(cart -> {
                    cart.getItems().clear();
                    cartRepository.save(cart);
                });
    }

    /**
     * 新しいカートを作成
     */
    private Cart createCart(String sessionId) {
        Cart cart = new Cart();
        cart.setSessionId(sessionId);
        cart.setCreatedAt(Instant.now());
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

}
