package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.JobFeedChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

public interface JobFeedChangeRepository extends JpaRepository<JobFeedChange, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<JobFeedChange> findByIdInAndProcessedAtIsNullOrderByChangedAtAscIdAsc(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<JobFeedChange> findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(
            LocalDateTime cutoff);
}
