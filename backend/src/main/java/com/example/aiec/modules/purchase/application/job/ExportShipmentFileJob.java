package com.example.aiec.modules.purchase.application.job;

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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportShipmentFileJob extends JobRunnerBase {

    private static final DateTimeFormatter FILE_TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Tokyo"));

    private final ShipmentRepository shipmentRepository;
    private final SftpPutJob sftpPutJob;
    private final JobScheduler jobScheduler;

    public ExportShipmentFileJob(JobRunHistoryRepository jobRunHistoryRepository,
                                 JobProperties jobProperties,
                                 ShipmentRepository shipmentRepository,
                                 SftpPutJob sftpPutJob,
                                 JobScheduler jobScheduler) {
        super(jobRunHistoryRepository, jobProperties);
        this.shipmentRepository = shipmentRepository;
        this.sftpPutJob = sftpPutJob;
        this.jobScheduler = jobScheduler;
    }

    @Job(name = "export-shipment-file", retries = 3)
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        int processed = execute(
                "export-shipment-file",
                getJobProperties().getEnabled().isExportShipmentFile(),
                this::exportFiles
        );

        if (processed > 0) {
            jobScheduler.enqueue(() -> sftpPutJob.run());
        }
    }

    private int exportFiles() {
        List<Shipment> shipments = shipmentRepository.findByStatusOrderByCreatedAtAsc(Shipment.ShipmentStatus.READY);
        int processed = 0;

        Path outputDir = Paths.get(getJobProperties().getExport().getOutputDir());
        Path backupDir = Paths.get(getJobProperties().getExport().getBackupDir());

        for (Shipment shipment : shipments) {
            String fileName = buildFileName(shipment.getId());
            Path outputFile = outputDir.resolve(fileName);
            Path tempFile = outputDir.resolve(fileName + ".tmp");
            Path backupFile = backupDir.resolve(fileName);

            try {
                Files.createDirectories(outputDir);
                Files.createDirectories(backupDir);

                String csv = buildCsv(shipment);
                Files.writeString(tempFile, csv, Charset.forName(getJobProperties().getExport().getCharset()));

                moveAtomically(tempFile, outputFile);
                Files.copy(outputFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

                shipment.setExportFilePath(outputFile.toString());
                shipment.setStatus(Shipment.ShipmentStatus.EXPORTED);
                shipmentRepository.save(shipment);
                processed++;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to export shipment file: shipmentId=" + shipment.getId(), e);
            }
        }

        return processed;
    }

    private String buildFileName(Long shipmentId) {
        String timestamp = FILE_TS_FORMATTER.format(Instant.now());
        String fileId = shipmentId + "-" + timestamp;
        return String.format(getJobProperties().getExport().getFileNamePattern(), fileId);
    }

    private String buildCsv(Shipment shipment) {
        String lineSeparator = "CRLF".equalsIgnoreCase(getJobProperties().getExport().getLineSeparator())
                ? "\r\n"
                : "\n";

        StringBuilder builder = new StringBuilder();
        builder.append("shipment_id,order_id,order_number,product_id,product_name,quantity,subtotal").append(lineSeparator);

        for (ShipmentItem item : shipment.getItems()) {
            builder
                    .append(shipment.getId()).append(',')
                    .append(shipment.getOrder().getId()).append(',')
                    .append(shipment.getOrder().getOrderNumber()).append(',')
                    .append(item.getProductId()).append(',')
                    .append(escape(item.getProductName())).append(',')
                    .append(item.getQuantity()).append(',')
                    .append(item.getSubtotal())
                    .append(lineSeparator);
        }

        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
