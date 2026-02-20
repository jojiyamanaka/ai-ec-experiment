package com.example.aiec.modules.purchase.application.job;

import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.purchase.shipment.entity.ShipmentItem;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.domain.entity.JobRunHistory;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportShipmentFileJobTest {

    @Mock
    private JobRunHistoryRepository jobRunHistoryRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private SftpPutJob sftpPutJob;

    @Mock
    private JobScheduler jobScheduler;

    @Captor
    private ArgumentCaptor<JobRunHistory> historyCaptor;

    @Captor
    private ArgumentCaptor<JobLambda> jobLambdaCaptor;

    @TempDir
    Path tempDir;

    private JobProperties jobProperties;
    private ExportShipmentFileJob exportShipmentFileJob;

    @BeforeEach
    void setUp() {
        jobProperties = new JobProperties();
        jobProperties.getExport().setOutputDir(tempDir.resolve("out").toString());
        jobProperties.getExport().setBackupDir(tempDir.resolve("backup").toString());
        exportShipmentFileJob = new ExportShipmentFileJob(
                jobRunHistoryRepository,
                jobProperties,
                shipmentRepository,
                sftpPutJob,
                jobScheduler
        );
    }

    @Test
    void run_shouldSkipWhenDisabled() {
        jobProperties.getEnabled().setExportShipmentFile(false);

        exportShipmentFileJob.run();

        verify(shipmentRepository, never()).findByStatusOrderByCreatedAtAsc(any());
        verify(jobScheduler, never()).enqueue(any(JobLambda.class));
        verify(jobRunHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(JobRunHistory.RunStatus.SKIPPED);
    }

    @Test
    void run_shouldExportCsvAndScheduleSftpWhenShipmentExists() throws Exception {
        Shipment shipment = buildShipment(100L, "商品,\"A\"");
        when(shipmentRepository.findByStatusOrderByCreatedAtAsc(Shipment.ShipmentStatus.READY))
                .thenReturn(List.of(shipment));

        exportShipmentFileJob.run();

        assertThat(shipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.EXPORTED);
        assertThat(shipment.getExportFilePath()).isNotBlank();

        Path outputFile = Path.of(shipment.getExportFilePath());
        assertThat(Files.exists(outputFile)).isTrue();
        assertThat(Files.exists(tempDir.resolve("backup").resolve(outputFile.getFileName()))).isTrue();

        String csv = Files.readString(outputFile);
        assertThat(csv).contains("shipment_id,order_id,order_number,product_id,product_name,quantity,subtotal");
        assertThat(csv).contains("\"商品,\"\"A\"\"\"");

        verify(shipmentRepository).save(eq(shipment));
        verify(jobScheduler).enqueue(jobLambdaCaptor.capture());

        verify(jobRunHistoryRepository).save(historyCaptor.capture());
        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SUCCESS);
        assertThat(history.getProcessedCount()).isEqualTo(1);
    }

    private Shipment buildShipment(Long shipmentId, String productName) {
        Order order = new Order();
        order.setId(50L);
        order.setOrderNumber("ORD-0000000050");

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrder(order);
        shipment.setStatus(Shipment.ShipmentStatus.READY);

        ShipmentItem item = new ShipmentItem();
        item.setShipment(shipment);
        item.setProductId(1L);
        item.setProductName(productName);
        item.setQuantity(2);
        item.setSubtotal(BigDecimal.valueOf(1980));
        shipment.setItems(List.of(item));

        return shipment;
    }
}
