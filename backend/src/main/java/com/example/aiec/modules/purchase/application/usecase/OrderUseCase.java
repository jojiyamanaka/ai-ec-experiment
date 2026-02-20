package com.example.aiec.modules.purchase.application.usecase;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.inventory.application.service.FrameAllocationService;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.purchase.application.port.OrderDto;
import com.example.aiec.modules.purchase.application.port.UnavailableProductDetail;
import com.example.aiec.modules.purchase.application.port.OrderCommandPort;
import com.example.aiec.modules.purchase.application.port.OrderQueryPort;
import com.example.aiec.modules.purchase.cart.entity.Cart;
import com.example.aiec.modules.purchase.cart.repository.CartRepository;
import com.example.aiec.modules.purchase.cart.service.CartService;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.InsufficientStockException;
import com.example.aiec.modules.shared.exception.ItemNotAvailableException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 注文ユースケース（Port実装）
 */
@Service
@RequiredArgsConstructor
class OrderUseCase implements OrderQueryPort, OrderCommandPort {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final InventoryCommandPort inventoryCommand;
    private final FrameAllocationService frameAllocationService;
    private final UserRepository userRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final MeterRegistry meterRegistry;

    private Counter orderCreatedCounter;
    private Counter orderCreationFailedCounter;
    private Counter inventoryReservationFailedCounter;
    private Timer orderCreationTimer;

    @PostConstruct
    private void initMetrics() {
        // `order.created` becomes `order_total` in Prometheus because `_created` is a reserved suffix.
        this.orderCreatedCounter = Counter.builder("order.created.count")
                .description("注文成功数").register(meterRegistry);
        this.orderCreationFailedCounter = Counter.builder("order.creation.failed")
                .description("注文失敗数").register(meterRegistry);
        this.inventoryReservationFailedCounter = Counter.builder("inventory.reservation.failed")
                .description("在庫引当失敗数").register(meterRegistry);
        this.orderCreationTimer = Timer.builder("order.creation.duration")
                .description("注文作成処理時間")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    /**
     * 注文を作成
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDto createOrder(String sessionId, String cartId, Long userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            if (!sessionId.equals(cartId)) {
                throw new BusinessException("INVALID_REQUEST", "無効なリクエストです");
            }

            Cart cart = cartRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("CART_NOT_FOUND", "カートが見つかりません"));

            if (cart.getItems().isEmpty()) {
                throw new BusinessException("CART_EMPTY", "カートが空です");
            }

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

            Order order = new Order();
            order.setOrderNumber(generateOrderNumber());
            order.setSessionId(sessionId);
            order.setTotalPrice(cart.getTotalPrice());
            order.setStatus(Order.OrderStatus.PENDING);

            if (userId != null) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "ユーザーが見つかりません"));
                order.setUser(user);
            }

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

            inventoryCommand.commitReservations(sessionId, savedOrder);
            outboxEventPublisher.publish("ORDER_PLACED", String.valueOf(savedOrder.getId()), Map.of(
                    "orderId", savedOrder.getId(),
                    "orderNumber", savedOrder.getOrderNumber()
            ));

            cartRepository.findBySessionId(sessionId)
                    .ifPresent(c -> {
                        c.getItems().clear();
                        cartRepository.save(c);
                    });

            orderCreatedCounter.increment();
            return OrderDto.fromEntity(savedOrder);

        } catch (Exception e) {
            if (e instanceof InsufficientStockException
                    || e instanceof ItemNotAvailableException) {
                inventoryReservationFailedCounter.increment();
            }
            orderCreationFailedCounter.increment();
            throw e;
        } finally {
            sample.stop(orderCreationTimer);
        }
    }

    /**
     * 注文をキャンセル
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDto cancelOrder(Long orderId, String sessionId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        boolean canAccess = false;

        if (order.getUser() != null) {
            canAccess = userId != null && order.getUser().getId().equals(userId);
        } else {
            canAccess = sessionId != null && order.getSessionId().equals(sessionId);
        }

        if (!canAccess) {
            throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
        }

        inventoryCommand.releaseCommittedReservations(orderId);

        order = orderRepository.findById(orderId).orElseThrow();
        return OrderDto.fromEntity(order);
    }

    /**
     * 注文詳細を取得
     */
    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public OrderDto getOrderById(Long id, String sessionId, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        boolean canAccess = false;

        if (order.getUser() != null) {
            canAccess = userId != null && order.getUser().getId().equals(userId);
        } else {
            canAccess = sessionId != null && order.getSessionId().equals(sessionId);
        }

        if (!canAccess) {
            throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
        }

        return OrderDto.fromEntity(order);
    }

    @Override
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

        // 同一トランザクションでOutboxに書き込む（コミット成功時のみイベントが残る）
        outboxEventPublisher.publish("ORDER_CONFIRMED", String.valueOf(order.getId()), Map.of(
            "orderId", order.getId(),
            "orderNumber", order.getOrderNumber(),
            "customerEmail", order.getUser() != null ? order.getUser().getEmail() : "",
            "totalPrice", order.getTotalPrice()
        ));

        return OrderDto.fromEntity(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDto markShipped(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        if (order.getStatus() != Order.OrderStatus.PREPARING_SHIPMENT) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                "この注文は発送完了にできません（現在のステータス: " + order.getStatus() + "）");
        }
        if (!isFullyCommitted(order)) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "この注文は発送完了にできません（現在のステータス: " + order.getStatus() + "）");
        }

        order.setStatus(Order.OrderStatus.SHIPPED);
        order = orderRepository.save(order);
        return OrderDto.fromEntity(order);
    }

    @Override
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDto retryAllocation(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        if (order.getStatus() == Order.OrderStatus.CANCELLED
                || order.getStatus() == Order.OrderStatus.SHIPPED
                || order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "この注文は引当再試行できません（現在のステータス: " + order.getStatus() + "）");
        }

        if (!isFullyCommitted(order)) {
            frameAllocationService.allocatePendingByOrderId(orderId);
        }

        Order refreshed = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));
        return OrderDto.fromEntity(refreshed);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<OrderDto> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderDto::fromEntity)
                .toList();
    }

    private String generateOrderNumber() {
        Long sequence = orderRepository.getNextOrderNumberSequence();
        return "ORD-" + String.format("%010d", sequence);
    }

    private boolean isFullyCommitted(Order order) {
        int orderedQuantity = order.getItems().stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
        int committedQuantity = order.getItems().stream()
                .mapToInt(item -> item.getCommittedQty() != null ? item.getCommittedQty() : 0)
                .sum();
        return orderedQuantity == committedQuantity;
    }

}
