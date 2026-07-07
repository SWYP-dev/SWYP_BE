package com.chwihap.server.domain.kanban.repository;

import com.chwihap.server.domain.kanban.entity.KanbanCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KanbanCardRepository extends JpaRepository<KanbanCard, Long> {

    boolean existsByJobPosting_Id(Long jobPostingId);
}
