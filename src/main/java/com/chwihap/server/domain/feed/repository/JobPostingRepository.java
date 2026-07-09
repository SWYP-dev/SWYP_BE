package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
}
