package com.example.aiec.modules.inventory.application.job;

import com.example.aiec.modules.inventory.domain.entity.LocationStock;
import com.example.aiec.modules.inventory.domain.repository.LocationStockRepository;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.repository.OrderItemRepository;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.JobRunnerBase;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 在庫引当整合性検証ジョブ
 */
@Service
@Slf4j
public class AllocationConsistencyCheckJob extends JobRunnerBase {

    private final LocationStockRepository locationStockRepository;
    private final OrderItemRepository orderItemRepository;

    public AllocationConsistencyCheckJob(JobRunHistoryRepository jobRunHistoryRepository,
                                         JobProperties jobProperties,
                                         LocationStockRepository locationStockRepository,
                                         OrderItemRepository orderItemRepository) {
        super(jobRunHistoryRepository, jobProperties);
        this.locationStockRepository = locationStockRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Job(name = "allocation-consistency-check", retries = 0)
    @Transactional(readOnly = true)
    public void run() {
        execute("allocation-consistency-check", true, this::checkConsistency);
    }

    int checkConsistency() {
        List<LocationStock> locationStocks = locationStockRepository.findAll();
        int mismatched = 0;

        for (LocationStock locationStock : locationStocks) {
            Long productId = locationStock.getProduct().getId();
            int expectedCommittedQty = orderItemRepository.sumCommittedQuantityByProductExcludingCancelled(
                    productId,
                    Order.OrderStatus.CANCELLED
            );
            int actualCommittedQty = locationStock.getCommittedQty() != null ? locationStock.getCommittedQty() : 0;
            if (actualCommittedQty != expectedCommittedQty) {
                mismatched++;
                log.warn("Commitment mismatch detected: productId={}, locationCommitted={}, orderItemsCommitted={}",
                        productId,
                        actualCommittedQty,
                        expectedCommittedQty);
            }
        }

        return mismatched;
    }
}
