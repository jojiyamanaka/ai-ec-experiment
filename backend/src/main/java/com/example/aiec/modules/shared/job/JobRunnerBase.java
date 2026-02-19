package com.example.aiec.modules.shared.job;

import com.example.aiec.modules.shared.job.domain.entity.JobRunHistory;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@RequiredArgsConstructor
public abstract class JobRunnerBase {

    private final JobRunHistoryRepository jobRunHistoryRepository;
    private final JobProperties jobProperties;

    protected int execute(String jobType, boolean enabled, Supplier<Integer> processor) {
        JobRunHistory history = new JobRunHistory();
        history.setRunId(UUID.randomUUID().toString());
        history.setJobType(jobType);
        history.setEnvironment(jobProperties.getEnv());
        history.setStartedAt(Instant.now());
        history.setProcessedCount(0);

        if (!enabled) {
            history.setStatus(JobRunHistory.RunStatus.SKIPPED);
            history.setFinishedAt(Instant.now());
            jobRunHistoryRepository.save(history);
            return 0;
        }

        try {
            int processedCount = processor.get();
            history.setProcessedCount(processedCount);
            history.setStatus(JobRunHistory.RunStatus.SUCCESS);
            history.setFinishedAt(Instant.now());
            jobRunHistoryRepository.save(history);
            return processedCount;
        } catch (Exception e) {
            history.setStatus(JobRunHistory.RunStatus.FAILED);
            history.setFinishedAt(Instant.now());
            history.setErrorMessage(truncateErrorMessage(e.getMessage()));
            jobRunHistoryRepository.save(history);
            throw e;
        }
    }

    protected JobProperties getJobProperties() {
        return jobProperties;
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= 2000) {
            return message;
        }
        return message.substring(0, 2000);
    }
}
