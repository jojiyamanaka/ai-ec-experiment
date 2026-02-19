package com.example.aiec.modules.shared.job;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jobs")
public class JobProperties {

    private String env = "local";

    private Enabled enabled = new Enabled();

    private Schedule schedule = new Schedule();

    private Export export = new Export();

    private Sftp sftp = new Sftp();

    @Data
    public static class Enabled {
        private boolean releaseReservations = true;
        private boolean createShipment = true;
        private boolean exportShipmentFile = true;
        private boolean sftpPut = true;
    }

    @Data
    public static class Schedule {
        private String releaseReservations = "0 */5 * * * *";
        private String createShipment = "0 0 1 * * *";
        private String exportShipmentFile = "0 */10 * * * *";
        private String sftpPut = "0 */15 * * * *";
    }

    @Data
    public static class Export {
        private String outputDir = "/tmp/aiec/shipments/out";
        private String backupDir = "/tmp/aiec/shipments/backup";
        private String fileNamePattern = "shipment-%s.csv";
        private String charset = "UTF-8";
        private String lineSeparator = "LF";
    }

    @Data
    public static class Sftp {
        private String strategy = "local";
        private String sentDir = "/tmp/aiec/shipments/sent";
    }
}
