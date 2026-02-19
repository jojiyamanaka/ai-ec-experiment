package com.example.aiec.modules.shared.job.transfer;

import java.io.IOException;
import java.nio.file.Path;

public interface TransferStrategy {

    void transfer(Path source, Path destination) throws IOException;
}
