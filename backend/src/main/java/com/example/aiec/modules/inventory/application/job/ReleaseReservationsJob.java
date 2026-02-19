package com.example.aiec.modules.inventory.application.job;

import com.example.aiec.modules.inventory.domain.entity.StockReservation;
import com.example.aiec.modules.inventory.domain.repository.StockReservationRepository;
import com.example.aiec.modules.shared.domain.model.ActorType;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.JobRunnerBase;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReleaseReservationsJob extends JobRunnerBase {

    private final StockReservationRepository stockReservationRepository;

    public ReleaseReservationsJob(JobRunHistoryRepository jobRunHistoryRepository,
                                  JobProperties jobProperties,
                                  StockReservationRepository stockReservationRepository) {
        super(jobRunHistoryRepository, jobProperties);
        this.stockReservationRepository = stockReservationRepository;
    }

    @Job(name = "release-reservations")
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        execute(
                "release-reservations",
                getJobProperties().getEnabled().isReleaseReservations(),
                () -> stockReservationRepository.softDeleteExpiredByType(
                        StockReservation.ReservationType.TENTATIVE,
                        Instant.now(),
                        ActorType.SYSTEM,
                        null
                )
        );
    }
}
