package com.example.aiec.modules.inventory.application.service;

import com.example.aiec.modules.inventory.domain.entity.LocationStock;
import com.example.aiec.modules.inventory.domain.repository.LocationStockRepository;
import com.example.aiec.modules.product.domain.entity.AllocationType;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import com.example.aiec.modules.purchase.order.repository.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrameAllocationServiceTest {

    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private LocationStockRepository locationStockRepository;

    @InjectMocks
    private FrameAllocationService frameAllocationService;

    @Test
    void allocatePendingByOrderId_noPendingItems_shouldReturnZero() {
        when(orderItemRepository.findPendingItemsForAllocationByOrderId(any(), any(), any())).thenReturn(List.of());

        int allocated = frameAllocationService.allocatePendingByOrderId(10L);

        assertThat(allocated).isZero();
        verify(locationStockRepository, never()).save(any());
    }

    @Test
    void allocatePendingByProductId_shouldAllocatePartiallyInFifoOrder() {
        Product product = new Product();
        product.setId(1L);
        product.setAllocationType(AllocationType.FRAME);

        Order order1 = new Order();
        order1.setId(100L);
        order1.setStatus(Order.OrderStatus.PENDING);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order1);
        item1.setProduct(product);
        item1.setQuantity(3);
        item1.setCommittedQty(0);

        Order order2 = new Order();
        order2.setId(101L);
        order2.setStatus(Order.OrderStatus.PENDING);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order2);
        item2.setProduct(product);
        item2.setQuantity(4);
        item2.setCommittedQty(1);

        LocationStock locationStock = new LocationStock();
        locationStock.setProduct(product);
        locationStock.setLocationId(1);
        locationStock.setAvailableQty(5);
        locationStock.setCommittedQty(0);

        when(orderItemRepository.findPendingItemsForAllocation(1L, AllocationType.FRAME, List.of(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.CONFIRMED,
                Order.OrderStatus.PREPARING_SHIPMENT
        ))).thenReturn(List.of(item1, item2));
        when(locationStockRepository.findByProductIdAndLocationIdForUpdate(1L, 1)).thenReturn(Optional.of(locationStock));

        int allocated = frameAllocationService.allocatePendingByProductId(1L);

        assertThat(allocated).isEqualTo(5);
        assertThat(item1.getCommittedQty()).isEqualTo(3);
        assertThat(item2.getCommittedQty()).isEqualTo(3);
        assertThat(locationStock.getCommittedQty()).isEqualTo(5);
        verify(locationStockRepository).save(locationStock);
        verify(orderItemRepository).saveAll(List.of(item1, item2));
    }

    @Test
    void allocatePendingByProductId_shouldCreateLocationStockWhenMissing() {
        Product product = new Product();
        product.setId(1L);
        product.setAllocationType(AllocationType.FRAME);

        Order order = new Order();
        order.setId(100L);
        order.setStatus(Order.OrderStatus.PENDING);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setCommittedQty(0);

        when(orderItemRepository.findPendingItemsForAllocation(1L, AllocationType.FRAME, List.of(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.CONFIRMED,
                Order.OrderStatus.PREPARING_SHIPMENT
        ))).thenReturn(List.of(item));
        when(locationStockRepository.findByProductIdAndLocationIdForUpdate(1L, 1)).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        LocationStock created = new LocationStock();
        created.setProduct(product);
        created.setLocationId(1);
        created.setAvailableQty(2);
        created.setCommittedQty(0);
        when(locationStockRepository.save(any(LocationStock.class))).thenReturn(created);

        frameAllocationService.allocatePendingByProductId(1L);

        verify(locationStockRepository, times(2)).save(any(LocationStock.class));
    }
}
