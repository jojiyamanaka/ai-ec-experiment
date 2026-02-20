package com.example.aiec.modules.inventory.application.service;

import com.example.aiec.modules.inventory.domain.entity.LocationStock;
import com.example.aiec.modules.inventory.domain.repository.LocationStockRepository;
import com.example.aiec.modules.product.domain.entity.AllocationType;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import com.example.aiec.modules.purchase.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 枠在庫商品の非同期本引当サービス
 */
@Service
@RequiredArgsConstructor
public class FrameAllocationService {

    private static final int DEFAULT_LOCATION_ID = 1;
    private static final List<Order.OrderStatus> ELIGIBLE_ORDER_STATUSES = List.copyOf(
            EnumSet.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED, Order.OrderStatus.PREPARING_SHIPMENT));

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final LocationStockRepository locationStockRepository;

    @Transactional(rollbackFor = Exception.class)
    public int allocatePendingByProductId(Long productId) {
        List<OrderItem> pendingItems = orderItemRepository.findPendingItemsForAllocation(
                productId,
                AllocationType.FRAME,
                ELIGIBLE_ORDER_STATUSES
        );
        return allocatePendingItems(pendingItems);
    }

    @Transactional(rollbackFor = Exception.class)
    public int allocatePendingByOrderId(Long orderId) {
        List<OrderItem> pendingItems = orderItemRepository.findPendingItemsForAllocationByOrderId(
                orderId,
                AllocationType.FRAME,
                ELIGIBLE_ORDER_STATUSES
        );
        return allocatePendingItems(pendingItems);
    }

    private int allocatePendingItems(List<OrderItem> pendingItems) {
        if (pendingItems.isEmpty()) {
            return 0;
        }

        Map<Long, List<OrderItem>> itemsByProduct = pendingItems.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getId()));

        int updatedCount = 0;
        for (Map.Entry<Long, List<OrderItem>> entry : itemsByProduct.entrySet()) {
            Long productId = entry.getKey();
            LocationStock locationStock = findOrCreateLocationStockForUpdate(productId);
            int remainingQty = locationStock.remainingQty();
            if (remainingQty <= 0) {
                continue;
            }

            for (OrderItem orderItem : entry.getValue()) {
                int currentAllocated = orderItem.getAllocatedQty() != null ? orderItem.getAllocatedQty() : 0;
                int orderQuantity = orderItem.getQuantity() != null ? orderItem.getQuantity() : 0;
                int shortfall = Math.max(0, orderQuantity - currentAllocated);
                if (shortfall == 0 || remainingQty == 0) {
                    continue;
                }

                int allocateQty = Math.min(shortfall, remainingQty);
                orderItem.setAllocatedQty(currentAllocated + allocateQty);
                locationStock.setAllocatedQty(locationStock.getAllocatedQty() + allocateQty);
                remainingQty -= allocateQty;
                updatedCount += allocateQty;
            }

            locationStockRepository.save(locationStock);
            orderItemRepository.saveAll(entry.getValue());
        }

        return updatedCount;
    }

    private LocationStock findOrCreateLocationStockForUpdate(Long productId) {
        return locationStockRepository.findByProductIdAndLocationIdForUpdate(productId, DEFAULT_LOCATION_ID)
                .orElseGet(() -> {
                    Product product = productRepository.findByIdForUpdate(productId)
                            .orElseThrow();
                    LocationStock locationStock = new LocationStock();
                    locationStock.setProduct(product);
                    locationStock.setLocationId(DEFAULT_LOCATION_ID);
                    locationStock.setAllocatableQty(0);
                    locationStock.setAllocatedQty(0);
                    return locationStockRepository.save(locationStock);
                });
    }
}
