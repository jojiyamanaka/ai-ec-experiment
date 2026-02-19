package com.example.aiec.modules.shared.job.transfer;

import com.example.aiec.modules.shared.job.JobProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferStrategyFactory {

    private final JobProperties jobProperties;
    private final LocalFileTransferStrategy localFileTransferStrategy;
    private final SftpTransferStrategy sftpTransferStrategy;

    public TransferStrategy resolve() {
        String strategy = jobProperties.getSftp().getStrategy();
        if ("local".equalsIgnoreCase(strategy)) {
            return localFileTransferStrategy;
        }
        if ("sftp".equalsIgnoreCase(strategy)) {
            return sftpTransferStrategy;
        }
        throw new IllegalArgumentException("Unsupported transfer strategy: " + strategy);
    }
}
