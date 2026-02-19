package com.example.aiec.modules.shared.job;

import com.example.aiec.modules.inventory.application.job.ReleaseReservationsJob;
import com.example.aiec.modules.purchase.application.job.CreateShipmentJob;
import com.example.aiec.modules.purchase.application.job.ExportShipmentFileJob;
import com.example.aiec.modules.purchase.application.job.SftpPutJob;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JobProperties.class)
@RequiredArgsConstructor
public class JobRunrConfig {

    private final JobScheduler jobScheduler;
    private final JobProperties jobProperties;
    private final ReleaseReservationsJob releaseReservationsJob;
    private final CreateShipmentJob createShipmentJob;
    private final ExportShipmentFileJob exportShipmentFileJob;
    private final SftpPutJob sftpPutJob;

    @PostConstruct
    public void registerRecurringJobs() {
        jobScheduler.scheduleRecurrently(
                "release-reservations",
                jobProperties.getSchedule().getReleaseReservations(),
                () -> releaseReservationsJob.run()
        );
        jobScheduler.scheduleRecurrently(
                "create-shipment",
                jobProperties.getSchedule().getCreateShipment(),
                () -> createShipmentJob.run()
        );
        jobScheduler.scheduleRecurrently(
                "export-shipment-file",
                jobProperties.getSchedule().getExportShipmentFile(),
                () -> exportShipmentFileJob.run()
        );
        jobScheduler.scheduleRecurrently(
                "sftp-put",
                jobProperties.getSchedule().getSftpPut(),
                () -> sftpPutJob.run()
        );
    }
}
