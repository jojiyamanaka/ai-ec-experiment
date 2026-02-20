package com.example.aiec.modules.purchase.application.job;

import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.domain.entity.JobRunHistory;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateShipmentJobTest {

    @Mock
    private JobRunHistoryRepository jobRunHistoryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ExportShipmentFileJob exportShipmentFileJob;

    @Mock
    private JobScheduler jobScheduler;

    @Captor
    private ArgumentCaptor<JobRunHistory> historyCaptor;

    @Captor
    private ArgumentCaptor<Shipment> shipmentCaptor;

    private JobProperties jobProperties;

    private CreateShipmentJob createShipmentJob;

    @BeforeEach
    void setUp() {
        jobProperties = new JobProperties();
        createShipmentJob = new CreateShipmentJob(
                jobRunHistoryRepository,
                jobProperties,
                orderRepository,
                shipmentRepository,
                exportShipmentFileJob,
                jobScheduler
        );
    }

    @Test
    void run_shouldRecordSkippedWhenDisabled() {
        jobProperties.getEnabled().setCreateShipment(false);

        createShipmentJob.run();

        verify(orderRepository, never()).findConfirmedWithoutOutboundShipment();
        verify(jobRunHistoryRepository).save(historyCaptor.capture());

        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SKIPPED);
        assertThat(history.getProcessedCount()).isEqualTo(0);
    }

    @Test
    void run_shouldBeIdempotentWhenShipmentAlreadyExists() {
        Order confirmedOrder = buildConfirmedOrder(10L);
        when(orderRepository.findConfirmedWithoutOutboundShipment()).thenReturn(List.of(confirmedOrder));
        when(shipmentRepository.existsByOrderIdAndShipmentType(10L, Shipment.ShipmentType.OUTBOUND)).thenReturn(true);

        createShipmentJob.run();

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(jobRunHistoryRepository).save(historyCaptor.capture());

        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SUCCESS);
        assertThat(history.getProcessedCount()).isEqualTo(0);
    }

    @Test
    void run_shouldCreateShipmentAndUpdateOrderStatus() {
        Order confirmedOrder = buildConfirmedOrder(20L);
        when(orderRepository.findConfirmedWithoutOutboundShipment()).thenReturn(List.of(confirmedOrder));
        when(shipmentRepository.existsByOrderIdAndShipmentType(20L, Shipment.ShipmentType.OUTBOUND)).thenReturn(false);

        createShipmentJob.run();

        verify(shipmentRepository).save(shipmentCaptor.capture());
        Shipment savedShipment = shipmentCaptor.getValue();

        assertThat(savedShipment.getOrder().getId()).isEqualTo(20L);
        assertThat(savedShipment.getShipmentType()).isEqualTo(Shipment.ShipmentType.OUTBOUND);
        assertThat(savedShipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.READY);
        assertThat(savedShipment.getItems()).hasSize(1);

        verify(orderRepository).save(eq(confirmedOrder));
        assertThat(confirmedOrder.getStatus()).isEqualTo(Order.OrderStatus.PREPARING_SHIPMENT);

        verify(jobRunHistoryRepository).save(historyCaptor.capture());
        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SUCCESS);
        assertThat(history.getProcessedCount()).isEqualTo(1);
    }

    private Order buildConfirmedOrder(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNumber("ORD-0000000001");
        order.setStatus(Order.OrderStatus.CONFIRMED);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setProductName("商品A");
        orderItem.setProductPrice(BigDecimal.valueOf(1000));
        orderItem.setQuantity(2);
        orderItem.setCommittedQty(2);
        orderItem.setSubtotal(BigDecimal.valueOf(2000));

        com.example.aiec.modules.product.domain.entity.Product product = new com.example.aiec.modules.product.domain.entity.Product();
        product.setId(999L);
        orderItem.setProduct(product);

        order.getItems().add(orderItem);
        return order;
    }
}
