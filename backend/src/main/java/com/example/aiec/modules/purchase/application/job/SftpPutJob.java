package com.example.aiec.modules.purchase.application.job;

import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.JobRunnerBase;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import com.example.aiec.modules.shared.job.transfer.TransferStrategy;
import com.example.aiec.modules.shared.job.transfer.TransferStrategyFactory;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class SftpPutJob extends JobRunnerBase {

    private final ShipmentRepository shipmentRepository;
    private final TransferStrategyFactory transferStrategyFactory;

    public SftpPutJob(JobRunHistoryRepository jobRunHistoryRepository,
                      JobProperties jobProperties,
                      ShipmentRepository shipmentRepository,
                      TransferStrategyFactory transferStrategyFactory) {
        super(jobRunHistoryRepository, jobProperties);
        this.shipmentRepository = shipmentRepository;
        this.transferStrategyFactory = transferStrategyFactory;
    }

    @Job(name = "sftp-put", retries = 3)
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        execute(
                "sftp-put",
                getJobProperties().getEnabled().isSftpPut(),
                this::transferFiles
        );
    }

    private int transferFiles() {
        List<Shipment> shipments = shipmentRepository.findByStatusOrderByCreatedAtAsc(Shipment.ShipmentStatus.EXPORTED);
        TransferStrategy transferStrategy = transferStrategyFactory.resolve();
        int processed = 0;

        for (Shipment shipment : shipments) {
            String exportFilePath = shipment.getExportFilePath();
            if (exportFilePath == null || exportFilePath.isBlank()) {
                continue;
            }

            if (shipmentRepository.existsByExportFilePathAndStatus(exportFilePath, Shipment.ShipmentStatus.TRANSFERRED)) {
                continue;
            }

            Path source = Paths.get(exportFilePath);
            if (!Files.exists(source)) {
                throw new IllegalStateException("Exported file does not exist: " + exportFilePath);
            }

            Path destination = Paths.get(getJobProperties().getSftp().getSentDir()).resolve(source.getFileName());

            try {
                transferStrategy.transfer(source, destination);
                shipment.setStatus(Shipment.ShipmentStatus.TRANSFERRED);
                shipmentRepository.save(shipment);
                processed++;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to transfer file: " + source, e);
            }
        }

        return processed;
    }
}
