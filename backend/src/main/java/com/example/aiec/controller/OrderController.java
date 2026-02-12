package com.example.aiec.controller;

import com.example.aiec.dto.*;
import com.example.aiec.entity.User;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.service.AuthService;
import com.example.aiec.service.CartService;
import com.example.aiec.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 注文・カートコントローラ
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final CartService cartService;
    private final OrderService orderService;
    private final AuthService authService;

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
     */
    @PostMapping
    public ApiResponse<OrderDto> createOrder(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateOrderRequest request) {

        // 認証済みの場合はuserIdを取得
        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = extractToken(authHeader);
                User user = authService.verifyToken(token);
                userId = user.getId();
            } catch (Exception e) {
                // 認証エラーの場合はゲスト注文として処理
                // （トークンが無効でもゲスト注文は許可）
            }
        }

        OrderDto order = orderService.createOrder(sessionId, request.getCartId(), userId);
        return ApiResponse.success(order);
    }

    /**
     * 注文詳細を取得
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderDto> getOrderById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 認証済みの場合はuserIdを取得
        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = extractToken(authHeader);
                User user = authService.verifyToken(token);
                userId = user.getId();
            } catch (Exception e) {
                // 認証エラーの場合はnullのまま
            }
        }

        OrderDto order = orderService.getOrderById(id, sessionId, userId);
        return ApiResponse.success(order);
    }

    /**
     * 会員の注文履歴を取得
     */
    @GetMapping("/history")
    public ApiResponse<List<OrderDto>> getOrderHistory(
            @RequestHeader("Authorization") String authHeader) {

        // トークンからユーザーIDを取得
        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);

        List<OrderDto> orders = orderService.getOrderHistory(user.getId());
        return ApiResponse.success(orders);
    }

    /**
     * 注文をキャンセル
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderDto> cancelOrder(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 認証済みの場合はuserIdを取得
        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = extractToken(authHeader);
                User user = authService.verifyToken(token);
                userId = user.getId();
            } catch (Exception e) {
                // 認証エラーの場合はnullのまま
            }
        }

        OrderDto order = orderService.cancelOrder(id, sessionId, userId);
        return ApiResponse.success(order);
    }

    /**
     * 注文確認（管理者向け）
     * POST /api/order/:id/confirm
     */
    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderDto> confirmOrder(@PathVariable Long id) {
        OrderDto order = orderService.confirmOrder(id);
        return ApiResponse.success(order);
    }

    /**
     * 注文発送（管理者向け）
     * POST /api/order/:id/ship
     */
    @PostMapping("/{id}/ship")
    public ApiResponse<OrderDto> shipOrder(@PathVariable Long id) {
        OrderDto order = orderService.shipOrder(id);
        return ApiResponse.success(order);
    }

    /**
     * 注文配達完了（管理者向け）
     * POST /api/order/:id/deliver
     */
    @PostMapping("/{id}/deliver")
    public ApiResponse<OrderDto> deliverOrder(@PathVariable Long id) {
        OrderDto order = orderService.deliverOrder(id);
        return ApiResponse.success(order);
    }

    /**
     * 全注文取得（管理者向け）
     * GET /api/order
     */
    @GetMapping
    public ApiResponse<java.util.List<OrderDto>> getAllOrders() {
        java.util.List<OrderDto> orders = orderService.getAllOrders();
        return ApiResponse.success(orders);
    }

    /**
     * Authorizationヘッダーからトークンを抽出
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

}
