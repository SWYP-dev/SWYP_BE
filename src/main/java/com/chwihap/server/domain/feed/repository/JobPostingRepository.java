package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    Optional<JobPosting> findByIdAndUser_Id(Long id, Long userId);

    Optional<JobPosting> findByUserIdAndSourcePlatformAndSourceExternalId(Long userId, JobPlatform sourcePlatform, String sourceExternalId);
}
