package com.example.aiec.modules.purchase.application.usecase;

import com.example.aiec.modules.purchase.application.port.CreateReturnRequest;
import com.example.aiec.modules.purchase.application.port.RejectReturnRequest;
import com.example.aiec.modules.purchase.application.port.ReturnCommandPort;
import com.example.aiec.modules.purchase.application.port.ReturnListResponse;
import com.example.aiec.modules.purchase.application.port.ReturnShipmentDto;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.purchase.shipment.entity.ShipmentItem;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.shared.domain.model.ActorType;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ConflictException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
class ReturnUseCase implements ReturnCommandPort, com.example.aiec.modules.purchase.application.port.ReturnQueryPort {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnShipmentDto createReturn(Long orderId, Long userId, CreateReturnRequest request) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        validateOrderOwner(order, userId);

        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_NOT_DELIVERED", "この注文は返品できません");
        }
        if (order.getDeliveredAt() == null || Instant.now().isAfter(order.getDeliveredAt().plus(30, ChronoUnit.DAYS))) {
            throw new BusinessException("RETURN_PERIOD_EXPIRED", "返品受付期間を過ぎています");
        }
        if (shipmentRepository.existsByOrderIdAndShipmentType(orderId, Shipment.ShipmentType.RETURN)) {
            throw new ConflictException("RETURN_ALREADY_EXISTS", "返品はすでに申請されています");
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShipmentType(Shipment.ShipmentType.RETURN);
        shipment.setStatus(Shipment.ShipmentStatus.RETURN_PENDING);
        shipment.setReason(request.getReason());
        applyActor(shipment, ActorType.USER, userId);

        for (CreateReturnRequest.Item itemRequest : request.getItems()) {
            OrderItem orderItem = order.getItems().stream()
                    .filter(item -> item.getId().equals(itemRequest.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("INVALID_ORDER_ITEM", "返品対象の明細が不正です"));

            if (itemRequest.getQuantity() > orderItem.getQuantity()) {
                throw new BusinessException("INVALID_RETURN_QUANTITY", "返品数量が不正です");
            }
            if (Boolean.FALSE.equals(orderItem.getProduct().getIsReturnable())) {
                throw new BusinessException("PRODUCT_NOT_RETURNABLE", "返品できない商品が含まれています");
            }

            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setOrderItem(orderItem);
            shipmentItem.setProductId(orderItem.getProduct().getId());
            shipmentItem.setProductName(orderItem.getProductName());
            shipmentItem.setProductPrice(orderItem.getProductPrice());
            shipmentItem.setQuantity(itemRequest.getQuantity());
            shipmentItem.setSubtotal(
                    orderItem.getProductPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity().longValue()))
            );
            shipment.addItem(shipmentItem);
        }

        Shipment savedShipment = shipmentRepository.save(shipment);
        publishOperation("RETURN_REQUEST", String.valueOf(userId), "/api/order/" + orderId + "/return", savedShipment.getId());
        return ReturnShipmentDto.fromEntity(savedShipment, order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnShipmentDto approveReturn(Long orderId) {
        Shipment shipment = getRequiredReturnShipment(orderId);
        ensureTransition(shipment, Shipment.ShipmentStatus.RETURN_PENDING, Shipment.ShipmentStatus.RETURN_APPROVED);
        shipment.setStatus(Shipment.ShipmentStatus.RETURN_APPROVED);
        shipment.setRejectionReason(null);
        applyActor(shipment, ActorType.BO_USER, null);
        Shipment savedShipment = shipmentRepository.save(shipment);
        publishOperation("RETURN_APPROVE", "admin", "/api/order/" + orderId + "/return/approve", savedShipment.getId());
        return ReturnShipmentDto.fromEntity(savedShipment, savedShipment.getOrder());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnShipmentDto rejectReturn(Long orderId, RejectReturnRequest request) {
        Shipment shipment = getRequiredReturnShipment(orderId);
        ensureTransition(shipment, Shipment.ShipmentStatus.RETURN_PENDING, Shipment.ShipmentStatus.RETURN_CANCELLED);
        shipment.setStatus(Shipment.ShipmentStatus.RETURN_CANCELLED);
        shipment.setRejectionReason(request.getReason());
        applyActor(shipment, ActorType.BO_USER, null);
        Shipment savedShipment = shipmentRepository.save(shipment);
        publishOperation("RETURN_REJECT", "admin", "/api/order/" + orderId + "/return/reject", savedShipment.getId());
        return ReturnShipmentDto.fromEntity(savedShipment, savedShipment.getOrder());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnShipmentDto confirmReturn(Long orderId) {
        Shipment shipment = getRequiredReturnShipment(orderId);
        ensureTransition(shipment, Shipment.ShipmentStatus.RETURN_APPROVED, Shipment.ShipmentStatus.RETURN_CONFIRMED);
        shipment.setStatus(Shipment.ShipmentStatus.RETURN_CONFIRMED);
        applyActor(shipment, ActorType.BO_USER, null);
        Shipment savedShipment = shipmentRepository.save(shipment);
        publishOperation("RETURN_CONFIRM", "admin", "/api/order/" + orderId + "/return/confirm", savedShipment.getId());
        return ReturnShipmentDto.fromEntity(savedShipment, savedShipment.getOrder());
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ReturnShipmentDto getReturnByOrderId(Long orderId, Long userId) {
        if (userId != null) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));
            validateOrderOwner(order, userId);
        }

        Shipment shipment = getRequiredReturnShipment(orderId);
        return ReturnShipmentDto.fromEntity(shipment, shipment.getOrder());
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ReturnListResponse getAllReturns(Shipment.ShipmentStatus status, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        PageRequest pageable = PageRequest.of(safePage - 1, safeLimit);
        Page<Shipment> shipmentPage = status == null
                ? shipmentRepository.findByShipmentTypeOrderByCreatedAtDesc(Shipment.ShipmentType.RETURN, pageable)
                : shipmentRepository.findByShipmentTypeAndStatusOrderByCreatedAtDesc(
                        Shipment.ShipmentType.RETURN,
                        status,
                        pageable
                );

        return new ReturnListResponse(
                shipmentPage.getContent().stream()
                        .map(shipment -> ReturnShipmentDto.fromEntity(shipment, shipment.getOrder()))
                        .toList(),
                new ReturnListResponse.Pagination(safePage, safeLimit, shipmentPage.getTotalElements())
        );
    }

    private Shipment getRequiredReturnShipment(Long orderId) {
        return shipmentRepository.findByOrderIdAndShipmentType(orderId, Shipment.ShipmentType.RETURN)
                .orElseThrow(() -> new ResourceNotFoundException("RETURN_NOT_FOUND", "返品情報が見つかりません"));
    }

    private void validateOrderOwner(Order order, Long userId) {
        if (userId == null || order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
        }
    }

    private void ensureTransition(
            Shipment shipment,
            Shipment.ShipmentStatus expectedStatus,
            Shipment.ShipmentStatus nextStatus
    ) {
        if (shipment.getStatus() != expectedStatus) {
            throw new BusinessException(
                    "INVALID_RETURN_STATUS_TRANSITION",
                    "返品ステータスを更新できません（現在のステータス: " + shipment.getStatus() + "）"
            );
        }
        if (nextStatus == Shipment.ShipmentStatus.RETURN_CANCELLED
                && shipment.getStatus() == Shipment.ShipmentStatus.RETURN_APPROVED) {
            throw new BusinessException(
                    "INVALID_RETURN_STATUS_TRANSITION",
                    "返品ステータスを更新できません（現在のステータス: " + shipment.getStatus() + "）"
            );
        }
    }

    private void applyActor(Shipment shipment, ActorType actorType, Long actorId) {
        if (shipment.getCreatedByType() == null) {
            shipment.setCreatedByType(actorType);
            shipment.setCreatedById(actorId);
        }
        shipment.setUpdatedByType(actorType);
        shipment.setUpdatedById(actorId);
    }

    private void publishOperation(String operationType, String performedBy, String requestPath, Long shipmentId) {
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", operationType,
                "performedBy", performedBy,
                "requestPath", requestPath,
                "details", "returnShipmentId=" + shipmentId
        ));
    }
}
