package com.example.aiec.service;

import com.example.aiec.dto.OrderDto;
import com.example.aiec.dto.UnavailableProductDetail;
import com.example.aiec.entity.Cart;
import com.example.aiec.entity.Order;
import com.example.aiec.entity.OrderItem;
import com.example.aiec.entity.User;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ItemNotAvailableException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.CartRepository;
import com.example.aiec.repository.OrderRepository;
import com.example.aiec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 注文サービス
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    /**
     * 注文を作成
     *
     * @param sessionId セッションID
     * @param cartId カートID
     * @param userId 会員ID（オプション、ログイン時のみ）
     * @return 作成された注文
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderDto createOrder(String sessionId, String cartId, Long userId) {
        // セッションIDとカートIDが一致するか確認
        if (!sessionId.equals(cartId)) {
            throw new BusinessException("INVALID_REQUEST", "無効なリクエストです");
        }

        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CART_NOT_FOUND", "カートが見つかりません"));

        // カートが空でないかチェック
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("CART_EMPTY", "カートが空です");
        }

        // 非公開商品チェック
        List<UnavailableProductDetail> unavailableProducts = cart.getItems().stream()
                .filter(item -> !item.getProduct().getIsPublished())
                .map(item -> new UnavailableProductDetail(
                        item.getProduct().getId(),
                        item.getProduct().getName()))
                .toList();

        if (!unavailableProducts.isEmpty()) {
            throw new ItemNotAvailableException(
                    "ITEM_NOT_AVAILABLE",
                    "購入できない商品がカートに含まれています",
                    unavailableProducts);
        }

        // 注文を作成
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setSessionId(sessionId);
        order.setTotalPrice(cart.getTotalPrice());
        order.setStatus(Order.OrderStatus.PENDING);

        // 会員IDを設定（ログイン時のみ）
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "ユーザーが見つかりません"));
            order.setUser(user);
        }

        // カートアイテムを注文アイテムに変換
        cart.getItems().forEach(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setProductPrice(cartItem.getProduct().getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(
                    cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity().longValue()))
            );
            order.addItem(orderItem);
        });

        Order savedOrder = orderRepository.save(order);

        // 仮引当 → 本引当に変換（stock 減少込み）
        inventoryService.commitReservations(sessionId, savedOrder);

        // カートをクリア（仮引当の解除はスキップ、既に本引当済み）
        cartRepository.findBySessionId(sessionId)
                .ifPresent(c -> {
                    c.getItems().clear();
                    cartRepository.save(c);
                });

        return OrderDto.fromEntity(savedOrder);
    }

    /**
     * 注文をキャンセル
     *
     * @param orderId 注文ID
     * @param sessionId セッションID（ゲスト注文の場合）
     * @param userId 会員ID（会員注文の場合）
     * @return キャンセルされた注文
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderDto cancelOrder(Long orderId, String sessionId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        // 権限チェック
        boolean canAccess = false;

        if (order.getUser() != null) {
            // 会員注文: userIdが一致すること
            canAccess = userId != null && order.getUser().getId().equals(userId);
        } else {
            // ゲスト注文: sessionIdが一致すること
            canAccess = sessionId != null && order.getSessionId().equals(sessionId);
        }

        if (!canAccess) {
            throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
        }

        // 本引当を解除（stock 戻し + ステータス変更込み）
        inventoryService.releaseCommittedReservations(orderId);

        // 注文を再取得（InventoryServiceで更新されたため）
        order = orderRepository.findById(orderId).orElseThrow();
        return OrderDto.fromEntity(order);
    }

    /**
     * 注文詳細を取得
     *
     * @param id 注文ID
     * @param sessionId セッションID（ゲスト注文の場合）
     * @param userId 会員ID（会員注文の場合）
     * @return 注文詳細
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public OrderDto getOrderById(Long id, String sessionId, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        // 権限チェック
        boolean canAccess = false;

        if (order.getUser() != null) {
            // 会員注文: userIdが一致すること
            canAccess = userId != null && order.getUser().getId().equals(userId);
        } else {
            // ゲスト注文: sessionIdが一致すること
            canAccess = sessionId != null && order.getSessionId().equals(sessionId);
        }

        if (!canAccess) {
            throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
        }

        return OrderDto.fromEntity(order);
    }

    /**
     * 注文を確認（PENDING → CONFIRMED）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderDto confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                "この注文は確認できません（現在のステータス: " + order.getStatus() + "）");
        }

        order.setStatus(Order.OrderStatus.CONFIRMED);
        order = orderRepository.save(order);
        return OrderDto.fromEntity(order);
    }

    /**
     * 注文を発送（CONFIRMED → SHIPPED）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderDto shipOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        if (order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                "この注文は発送できません（現在のステータス: " + order.getStatus() + "）");
        }

        order.setStatus(Order.OrderStatus.SHIPPED);
        order = orderRepository.save(order);
        return OrderDto.fromEntity(order);
    }

    /**
     * 注文を配達完了（SHIPPED → DELIVERED）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderDto deliverOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        if (order.getStatus() != Order.OrderStatus.SHIPPED) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                "この注文は配達完了にできません（現在のステータス: " + order.getStatus() + "）");
        }

        order.setStatus(Order.OrderStatus.DELIVERED);
        order = orderRepository.save(order);
        return OrderDto.fromEntity(order);
    }

    /**
     * 全注文を取得（管理者用）
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public java.util.List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderDto::fromEntity)
                .toList();
    }

    /**
     * 会員の注文履歴を取得
     *
     * @param userId 会員ID（認証済み）
     * @return 注文履歴一覧（作成日時降順）
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<OrderDto> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderDto::fromEntity)
                .toList();
    }

    /**
     * 注文番号を生成（ORD-xxxxxxxxxx形式）
     */
    private String generateOrderNumber() {
        Long sequence = orderRepository.getNextOrderNumberSequence();
        return "ORD-" + String.format("%010d", sequence);
    }

}
