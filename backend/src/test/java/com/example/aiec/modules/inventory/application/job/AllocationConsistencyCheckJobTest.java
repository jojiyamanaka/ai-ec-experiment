package com.example.aiec.modules.inventory.application.job;

import com.example.aiec.modules.inventory.domain.entity.LocationStock;
import com.example.aiec.modules.inventory.domain.repository.LocationStockRepository;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.repository.OrderItemRepository;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.domain.entity.JobRunHistory;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationConsistencyCheckJobTest {

    @Mock private JobRunHistoryRepository jobRunHistoryRepository;
    @Mock private LocationStockRepository locationStockRepository;
    @Mock private OrderItemRepository orderItemRepository;

    @Captor private ArgumentCaptor<JobRunHistory> historyCaptor;

    private AllocationConsistencyCheckJob job;

    @BeforeEach
    void setUp() {
        job = new AllocationConsistencyCheckJob(
                jobRunHistoryRepository,
                new JobProperties(),
                locationStockRepository,
                orderItemRepository
        );
    }

    @Test
    void run_whenNoMismatch_shouldRecordSuccessWithZeroProcessed() {
        LocationStock stock = buildLocationStock(1L, 3);
        when(locationStockRepository.findAll()).thenReturn(List.of(stock));
        when(orderItemRepository.sumAllocatedQuantityByProductExcludingCancelled(1L, Order.OrderStatus.CANCELLED))
                .thenReturn(3);

        job.run();

        verify(jobRunHistoryRepository).save(historyCaptor.capture());
        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SUCCESS);
        assertThat(history.getProcessedCount()).isEqualTo(0);
    }

    @Test
    void run_whenMismatch_shouldRecordProcessedCount() {
        LocationStock stock = buildLocationStock(1L, 5);
        when(locationStockRepository.findAll()).thenReturn(List.of(stock));
        when(orderItemRepository.sumAllocatedQuantityByProductExcludingCancelled(1L, Order.OrderStatus.CANCELLED))
                .thenReturn(2);

        job.run();

        verify(jobRunHistoryRepository).save(historyCaptor.capture());
        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SUCCESS);
        assertThat(history.getProcessedCount()).isEqualTo(1);
    }

    private LocationStock buildLocationStock(Long productId, Integer allocatedQty) {
        Product product = new Product();
        product.setId(productId);

        LocationStock stock = new LocationStock();
        stock.setProduct(product);
        stock.setAllocatedQty(allocatedQty);
        stock.setAllocatableQty(allocatedQty);
        stock.setLocationId(1);
        return stock;
    }
}
