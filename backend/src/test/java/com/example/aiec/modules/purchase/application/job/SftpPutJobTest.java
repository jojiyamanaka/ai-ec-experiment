package com.example.aiec.modules.purchase.application.job;

import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.domain.entity.JobRunHistory;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import com.example.aiec.modules.shared.job.transfer.TransferStrategy;
import com.example.aiec.modules.shared.job.transfer.TransferStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SftpPutJobTest {

    @Mock
    private JobRunHistoryRepository jobRunHistoryRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TransferStrategyFactory transferStrategyFactory;

    @Mock
    private TransferStrategy transferStrategy;

    @Captor
    private ArgumentCaptor<JobRunHistory> historyCaptor;

    @TempDir
    Path tempDir;

    private JobProperties jobProperties;
    private SftpPutJob sftpPutJob;

    @BeforeEach
    void setUp() {
        jobProperties = new JobProperties();
        jobProperties.getSftp().setSentDir(tempDir.resolve("sent").toString());
        sftpPutJob = new SftpPutJob(
                jobRunHistoryRepository,
                jobProperties,
                shipmentRepository,
                transferStrategyFactory
        );
    }

    @Test
    void run_shouldSkipWhenDisabled() {
        jobProperties.getEnabled().setSftpPut(false);

        sftpPutJob.run();

        verify(shipmentRepository, never()).findByStatusOrderByCreatedAtAsc(any());
        verify(jobRunHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(JobRunHistory.RunStatus.SKIPPED);
    }

    @Test
    void run_shouldTransferFileAndUpdateStatus() throws Exception {
        Path source = tempDir.resolve("shipment-100.csv");
        Files.writeString(source, "dummy");

        Shipment shipment = new Shipment();
        shipment.setId(100L);
        shipment.setStatus(Shipment.ShipmentStatus.EXPORTED);
        shipment.setExportFilePath(source.toString());

        when(shipmentRepository.findByStatusOrderByCreatedAtAsc(Shipment.ShipmentStatus.EXPORTED))
                .thenReturn(List.of(shipment));
        when(shipmentRepository.existsByExportFilePathAndStatus(source.toString(), Shipment.ShipmentStatus.TRANSFERRED))
                .thenReturn(false);
        when(transferStrategyFactory.resolve()).thenReturn(transferStrategy);

        sftpPutJob.run();

        verify(transferStrategy).transfer(eq(source), eq(tempDir.resolve("sent").resolve(source.getFileName())));
        verify(shipmentRepository).save(shipment);
        assertThat(shipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.TRANSFERRED);

        verify(jobRunHistoryRepository).save(historyCaptor.capture());
        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SUCCESS);
        assertThat(history.getProcessedCount()).isEqualTo(1);
    }

    @Test
    void run_whenExportFileDoesNotExist_shouldFail() {
        String missingPath = tempDir.resolve("missing.csv").toString();

        Shipment shipment = new Shipment();
        shipment.setId(101L);
        shipment.setStatus(Shipment.ShipmentStatus.EXPORTED);
        shipment.setExportFilePath(missingPath);

        when(shipmentRepository.findByStatusOrderByCreatedAtAsc(Shipment.ShipmentStatus.EXPORTED))
                .thenReturn(List.of(shipment));
        when(shipmentRepository.existsByExportFilePathAndStatus(missingPath, Shipment.ShipmentStatus.TRANSFERRED))
                .thenReturn(false);
        when(transferStrategyFactory.resolve()).thenReturn(transferStrategy);

        assertThatThrownBy(() -> sftpPutJob.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exported file does not exist");

        verify(jobRunHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(JobRunHistory.RunStatus.FAILED);
    }
}
