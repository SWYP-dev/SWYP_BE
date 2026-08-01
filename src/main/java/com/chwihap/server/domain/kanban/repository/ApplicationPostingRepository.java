package com.chwihap.server.domain.kanban.repository;

import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationPostingRepository extends JpaRepository<ApplicationPosting, Long> {
}
