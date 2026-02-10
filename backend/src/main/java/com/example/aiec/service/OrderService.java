package com.example.aiec.service;

import com.example.aiec.dto.OrderDto;
import com.example.aiec.entity.Cart;
import com.example.aiec.entity.Order;
import com.example.aiec.entity.OrderItem;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.CartRepository;
import com.example.aiec.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    /**
     * 注文を作成
     */
    @Transactional
    public OrderDto createOrder(String sessionId, String cartId) {
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

        // 注文を作成
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setSessionId(sessionId);
        order.setTotalPrice(cart.getTotalPrice());
        order.setStatus(Order.OrderStatus.PENDING);

        // カートアイテムを注文アイテムに変換
        cart.getItems().forEach(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(cartItem.getProduct().getPrice() * cartItem.getQuantity());
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
     * 注文キャンセル
     */
    @Transactional
    public void cancelOrder(Long orderId, String sessionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        // セッションIDが一致するか確認
        if (!order.getSessionId().equals(sessionId)) {
            throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
        }

        // 本引当を解除（stock 戻し + ステータス変更込み）
        inventoryService.releaseCommittedReservations(orderId);
    }

    /**
     * 注文詳細を取得
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id, String sessionId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        // セッションIDが一致するか確認
        if (!order.getSessionId().equals(sessionId)) {
            throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
        }

        return OrderDto.fromEntity(order);
    }

    /**
     * 注文番号を生成（ORD-YYYYMMDD-XXX形式）
     */
    private String generateOrderNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ORD-" + dateStr + "-";

        // 今日の注文数を取得して、連番を生成
        long todayOrderCount = orderRepository.findAll().stream()
                .filter(o -> o.getOrderNumber().startsWith(prefix))
                .count();

        int sequence = (int) (todayOrderCount + 1);
        return prefix + String.format("%03d", sequence);
    }

}
