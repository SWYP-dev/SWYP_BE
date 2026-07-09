package com.chwihap.server.domain.kanban.repository;

import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KanbanCardRepository extends JpaRepository<KanbanCard, Long> {

    long countByStage(KanbanStage stage);

    @Query("""
            SELECT c FROM KanbanCard c
            JOIN FETCH c.jobPosting
            WHERE c.user.id = :userId
            ORDER BY c.position ASC
            """)
    List<KanbanCard> findByUser_IdOrderByPositionAsc(@Param("userId") Long userId);

    boolean existsByUser_IdAndJobPosting_Id(Long userId, Long jobPostingId);

    @Query("""
            SELECT COALESCE(MAX(c.position), 0)
            FROM KanbanCard c
            WHERE c.stage = :stage
            """)
    int findMaxPositionByStage(@Param("stage") KanbanStage stage);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE KanbanCard c
            SET c.stage = :moveToStage,
                c.position = c.position + :positionOffset
            WHERE c.stage = :deleteStage
            """)
    int moveCardsToStage(
            @Param("deleteStage") KanbanStage deleteStage,
            @Param("moveToStage") KanbanStage moveToStage,
            @Param("positionOffset") int positionOffset
    );

    @Query("""
            SELECT c FROM KanbanCard c
            JOIN FETCH c.jobPosting jp
            WHERE c.user.id = :userId
            ORDER BY jp.deadline ASC
            """)
    List<KanbanCard> findByUserIdOrderByJobPostingDeadlineAsc(@Param("userId") Long userId);
}
