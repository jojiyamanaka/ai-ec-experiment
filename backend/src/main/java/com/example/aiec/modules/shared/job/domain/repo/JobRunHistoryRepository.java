package com.example.aiec.modules.shared.job.domain.repo;

import com.example.aiec.modules.shared.job.domain.entity.JobRunHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRunHistoryRepository extends JpaRepository<JobRunHistory, Long> {
}
