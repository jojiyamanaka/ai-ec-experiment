package com.example.aiec.modules.purchase.application.job;

import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.purchase.shipment.entity.ShipmentItem;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.JobRunnerBase;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreateShipmentJob extends JobRunnerBase {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ExportShipmentFileJob exportShipmentFileJob;
    private final JobScheduler jobScheduler;

    public CreateShipmentJob(JobRunHistoryRepository jobRunHistoryRepository,
                             JobProperties jobProperties,
                             OrderRepository orderRepository,
                             ShipmentRepository shipmentRepository,
                             ExportShipmentFileJob exportShipmentFileJob,
                             JobScheduler jobScheduler) {
        super(jobRunHistoryRepository, jobProperties);
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.exportShipmentFileJob = exportShipmentFileJob;
        this.jobScheduler = jobScheduler;
    }

    @Job(name = "create-shipment", retries = 3)
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        int processed = execute(
                "create-shipment",
                getJobProperties().getEnabled().isCreateShipment(),
                this::createShipments
        );

        if (processed > 0) {
            jobScheduler.enqueue(() -> exportShipmentFileJob.run());
        }
    }

    private int createShipments() {
        List<Order> orders = orderRepository.findConfirmedWithoutOutboundShipment();
        int processed = 0;

        for (Order order : orders) {
            if (shipmentRepository.existsByOrderIdAndShipmentType(order.getId(), Shipment.ShipmentType.OUTBOUND)) {
                continue;
            }
            boolean hasUncommittedItem = order.getItems().stream()
                    .anyMatch(item -> (item.getCommittedQty() != null ? item.getCommittedQty() : 0)
                            < (item.getQuantity() != null ? item.getQuantity() : 0));
            if (hasUncommittedItem) {
                continue;
            }

            Shipment shipment = new Shipment();
            shipment.setOrder(order);
            shipment.setShipmentType(Shipment.ShipmentType.OUTBOUND);
            shipment.setStatus(Shipment.ShipmentStatus.READY);

            for (OrderItem orderItem : order.getItems()) {
                ShipmentItem shipmentItem = new ShipmentItem();
                shipmentItem.setOrderItem(orderItem);
                shipmentItem.setProductId(orderItem.getProduct().getId());
                shipmentItem.setProductName(orderItem.getProductName());
                shipmentItem.setProductPrice(orderItem.getProductPrice());
                shipmentItem.setQuantity(orderItem.getQuantity());
                shipmentItem.setSubtotal(orderItem.getSubtotal());
                shipment.addItem(shipmentItem);
            }

            shipmentRepository.save(shipment);
            order.setStatus(Order.OrderStatus.PREPARING_SHIPMENT);
            orderRepository.save(order);
            processed++;
        }

        return processed;
    }
}
