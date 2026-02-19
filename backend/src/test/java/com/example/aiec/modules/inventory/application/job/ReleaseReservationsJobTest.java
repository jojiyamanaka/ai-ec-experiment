package com.example.aiec.modules.inventory.application.job;

import com.example.aiec.modules.inventory.domain.entity.StockReservation;
import com.example.aiec.modules.inventory.domain.repository.StockReservationRepository;
import com.example.aiec.modules.shared.domain.model.ActorType;
import com.example.aiec.modules.shared.job.JobProperties;
import com.example.aiec.modules.shared.job.domain.entity.JobRunHistory;
import com.example.aiec.modules.shared.job.domain.repo.JobRunHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseReservationsJobTest {

    @Mock
    private JobRunHistoryRepository jobRunHistoryRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Captor
    private ArgumentCaptor<JobRunHistory> historyCaptor;

    private JobProperties jobProperties;

    private ReleaseReservationsJob releaseReservationsJob;

    @BeforeEach
    void setUp() {
        jobProperties = new JobProperties();
        releaseReservationsJob = new ReleaseReservationsJob(jobRunHistoryRepository, jobProperties, stockReservationRepository);
    }

    @Test
    void run_shouldRecordSkippedWhenDisabled() {
        jobProperties.getEnabled().setReleaseReservations(false);

        releaseReservationsJob.run();

        verify(stockReservationRepository, never()).softDeleteExpiredByType(any(), any(), any(), any());
        verify(jobRunHistoryRepository).save(historyCaptor.capture());

        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SKIPPED);
        assertThat(history.getProcessedCount()).isEqualTo(0);
    }

    @Test
    void run_shouldRecordProcessedCountWhenExecuted() {
        when(stockReservationRepository.softDeleteExpiredByType(
                eq(StockReservation.ReservationType.TENTATIVE),
                any(Instant.class),
                eq(ActorType.SYSTEM),
                eq(null)
        )).thenReturn(4);

        releaseReservationsJob.run();

        verify(stockReservationRepository).softDeleteExpiredByType(
                eq(StockReservation.ReservationType.TENTATIVE),
                any(Instant.class),
                eq(ActorType.SYSTEM),
                eq(null)
        );
        verify(jobRunHistoryRepository).save(historyCaptor.capture());

        JobRunHistory history = historyCaptor.getValue();
        assertThat(history.getStatus()).isEqualTo(JobRunHistory.RunStatus.SUCCESS);
        assertThat(history.getProcessedCount()).isEqualTo(4);
    }
}
