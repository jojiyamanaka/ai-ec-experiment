package com.example.aiec.modules.shared.outbox.domain.repository;

import com.example.aiec.modules.shared.outbox.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = 'PENDING'
          AND e.scheduledAt <= :now
        ORDER BY e.scheduledAt ASC
        LIMIT 50
        """)
    List<OutboxEvent> findPendingEvents(Instant now);
}
