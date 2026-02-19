package com.example.aiec.modules.shared.job.transfer;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class SftpTransferStrategy implements TransferStrategy {

    @Override
    public void transfer(Path source, Path destination) throws IOException {
        throw new UnsupportedOperationException("SFTP transfer strategy is not implemented yet");
    }
}
