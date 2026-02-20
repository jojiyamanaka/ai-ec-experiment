package com.example.aiec.modules.purchase.adapter.rest;

import com.example.aiec.modules.purchase.adapter.dto.CartDto;
import com.example.aiec.modules.purchase.adapter.dto.CartItemDto;
import com.example.aiec.modules.purchase.adapter.dto.AddToCartRequest;
import com.example.aiec.modules.purchase.adapter.dto.UpdateQuantityRequest;
import com.example.aiec.modules.purchase.application.port.OrderDto;
import com.example.aiec.modules.purchase.adapter.dto.CreateOrderRequest;
import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.customer.domain.service.AuthService;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.purchase.cart.service.CartService;
import com.example.aiec.modules.purchase.application.port.OrderCommandPort;
import com.example.aiec.modules.purchase.application.port.OrderQueryPort;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 注文・カートコントローラ
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "注文", description = "注文の作成・取得・状態更新")
public class OrderController {

    private final CartService cartService;
    private final OrderCommandPort orderCommand;
    private final OrderQueryPort orderQuery;
    private final AuthService authService;
    private final BoAuthService boAuthService;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * カート取得
     * GET /api/order/cart
     */
    @GetMapping("/cart")
    @Operation(summary = "カート取得", description = "セッションに紐づくカートを取得")
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
    @Operation(summary = "カートに商品追加", description = "カートに商品を追加")
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
    @Operation(summary = "カート内商品の数量変更", description = "カート内商品の数量を変更")
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
    @Operation(summary = "カートから商品削除", description = "カートから商品を削除")
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
    @Operation(summary = "注文作成", description = "カートの内容で注文を作成")
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

        OrderDto order = orderCommand.createOrder(sessionId, request.getCartId(), userId);

        // 監査ログ記録イベント発行
        final Long finalUserId = userId;
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ORDER_CREATE",
                "performedBy", finalUserId != null ? finalUserId.toString() : "guest",
                "requestPath", "/api/order",
                "details", "orderId=" + order.getOrderId()));

        return ApiResponse.success(order);
    }

    /**
     * 注文詳細を取得
     */
    @GetMapping("/{id}")
    @Operation(summary = "注文詳細取得", description = "指定IDの注文詳細を取得")
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

        OrderDto order = orderQuery.getOrderById(id, sessionId, userId);
        return ApiResponse.success(order);
    }

    /**
     * 会員の注文履歴を取得
     */
    @GetMapping("/history")
    @Operation(summary = "注文履歴取得", description = "会員の注文履歴を取得")
    public ApiResponse<List<OrderDto>> getOrderHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // トークンからユーザーIDを取得
        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);

        List<OrderDto> orders = orderQuery.getOrderHistory(user.getId());
        return ApiResponse.success(orders);
    }

    /**
     * 注文をキャンセル
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "注文キャンセル", description = "注文をキャンセルし在庫を戻す")
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

        OrderDto order = orderCommand.cancelOrder(id, sessionId, userId);

        // 監査ログ記録イベント発行
        final Long finalUserId = userId;
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ORDER_CANCEL",
                "performedBy", finalUserId != null ? finalUserId.toString() : "guest",
                "requestPath", "/api/order/" + id + "/cancel",
                "details", "orderId=" + id));

        return ApiResponse.success(order);
    }

    /**
     * 注文確認（管理者向け）
     * POST /api/order/:id/confirm
     */
    @PostMapping("/{id}/confirm")
    @Operation(summary = "注文確認", description = "管理者が注文を確認済みにする")
    public ApiResponse<OrderDto> confirmOrder(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 認証・認可チェック
        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/order/" + id + "/confirm");

        // 管理操作実行
        OrderDto order = orderCommand.confirmOrder(id);

        // 監査ログ記録イベント発行
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ORDER_CONFIRM",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/order/" + id + "/confirm",
                "details", "Confirmed order: " + order.getOrderNumber()));

        return ApiResponse.success(order);
    }

    /**
     * 注文発送完了（管理者向け）
     * POST /api/order/:id/mark-shipped
     */
    @PostMapping("/{id}/mark-shipped")
    @Operation(summary = "注文発送完了", description = "管理者が注文を発送完了にする")
    public ApiResponse<OrderDto> markShipped(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 認証・認可チェック
        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/order/" + id + "/mark-shipped");

        // 管理操作実行
        OrderDto order = orderCommand.markShipped(id);

        // 監査ログ記録イベント発行
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ORDER_MARK_SHIPPED",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/order/" + id + "/mark-shipped",
                "details", "Marked order shipped: " + order.getOrderNumber()));

        return ApiResponse.success(order);
    }

    /**
     * 引当再試行（管理者向け）
     * POST /api/order/:id/allocation/retry
     */
    @PostMapping("/{id}/allocation/retry")
    @Operation(summary = "本引当再試行", description = "管理者が注文の本引当を再試行する")
    public ApiResponse<OrderDto> retryAllocation(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/order/" + id + "/allocation/retry");

        OrderDto order = orderCommand.retryAllocation(id);
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ORDER_ALLOCATION_RETRY",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/order/" + id + "/allocation/retry",
                "details", "Retried allocation: " + order.getOrderNumber()));

        return ApiResponse.success(order);
    }

    /**
     * 注文配達完了（管理者向け）
     * POST /api/order/:id/deliver
     */
    @PostMapping("/{id}/deliver")
    @Operation(summary = "注文配達完了", description = "管理者が注文を配達完了にする")
    public ApiResponse<OrderDto> deliverOrder(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 認証・認可チェック
        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/order/" + id + "/deliver");

        // 管理操作実行
        OrderDto order = orderCommand.deliverOrder(id);

        // 監査ログ記録イベント発行
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ORDER_DELIVER",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/order/" + id + "/deliver",
                "details", "Delivered order: " + order.getOrderNumber()));

        return ApiResponse.success(order);
    }

    /**
     * 全注文取得（管理者向け）
     * GET /api/order
     */
    @GetMapping
    @Operation(summary = "全注文取得", description = "管理者が全注文を取得")
    public ApiResponse<java.util.List<OrderDto>> getAllOrders(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 認証・認可チェック
        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/order");

        // 管理操作実行
        java.util.List<OrderDto> orders = orderQuery.getAllOrders();

        // 監査ログ記録イベント発行
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ADMIN_ACTION",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/order",
                "details", "Retrieved all orders (count: " + orders.size() + ")"));

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

    /**
     * 管理者権限チェック
     */
    private void requireAdmin(BoUser boUser, String requestPath) {
        if (boUser.getPermissionLevel() != PermissionLevel.ADMIN
                && boUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                    "operationType", "AUTHORIZATION_ERROR",
                    "performedBy", boUser.getEmail(),
                    "requestPath", requestPath,
                    "details", "BoUser attempted to access admin resource without permission"));
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

}
