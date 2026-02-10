package com.example.aiec.controller;

import com.example.aiec.dto.*;
import com.example.aiec.service.CartService;
import com.example.aiec.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 注文・カートコントローラ
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final CartService cartService;
    private final OrderService orderService;

    /**
     * カート取得
     * GET /api/order/cart
     */
    @GetMapping("/cart")
    public ApiResponse<CartDto> getCart(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        CartDto cart = cartService.getOrCreateCart(sessionId);
        return ApiResponse.success(cart);
    }

    /**
     * カートに商品追加
     * POST /api/order/cart/items
     */
    @PostMapping("/cart/items")
    public ApiResponse<CartDto> addToCart(
            @RequestHeader("X-Session-Id") String sessionId,
            @Valid @RequestBody AddToCartRequest request
    ) {
        CartDto cart = cartService.addToCart(sessionId, request);
        return ApiResponse.success(cart);
    }

    /**
     * カート内商品の数量変更
     * PUT /api/order/cart/items/:id
     */
    @PutMapping("/cart/items/{id}")
    public ApiResponse<CartDto> updateCartItem(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuantityRequest request
    ) {
        CartDto cart = cartService.updateCartItemQuantity(sessionId, id, request.getQuantity());
        return ApiResponse.success(cart);
    }

    /**
     * カートから商品削除
     * DELETE /api/order/cart/items/:id
     */
    @DeleteMapping("/cart/items/{id}")
    public ApiResponse<CartDto> removeFromCart(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable Long id
    ) {
        CartDto cart = cartService.removeFromCart(sessionId, id);
        return ApiResponse.success(cart);
    }

    /**
     * 注文作成
     * POST /api/order
     */
    @PostMapping
    public ApiResponse<OrderDto> createOrder(
            @RequestHeader("X-Session-Id") String sessionId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderDto order = orderService.createOrder(sessionId, request.getCartId());
        return ApiResponse.success(order);
    }

    /**
     * 注文詳細取得
     * GET /api/order/:id
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderDto> getOrder(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable Long id
    ) {
        OrderDto order = orderService.getOrderById(id, sessionId);
        return ApiResponse.success(order);
    }

}
