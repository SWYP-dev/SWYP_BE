package com.chwihap.server.domain.kanban.repository;

import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KanbanCardRepository extends JpaRepository<KanbanCard, Long> {

    boolean existsByJobPosting_Id(Long jobPostingId);

    Optional<KanbanCard> findByIdAndUser_Id(Long cardId, Long userId);

    long countByStage(KanbanStage stage);

    @Query("""
            SELECT c FROM KanbanCard c
            JOIN FETCH c.jobPosting
            WHERE c.user.id = :userId
            ORDER BY c.position ASC
            """)
    List<KanbanCard> findByUser_IdOrderByPositionAsc(@Param("userId") Long userId);

    boolean existsByUser_IdAndJobPosting_Id(Long userId, Long jobPostingId);

    boolean existsByUser_IdAndJobPosting_OriginalUrl(Long userId, String url);

    boolean existsByUser_IdAndJobPosting_OriginalUrlAndIdNot(Long userId, String url, Long cardId);

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

    @Modifying
    @Query(value = """
            UPDATE kanban_cards
            SET position = :position
            WHERE id = :cardId
            """, nativeQuery = true)
    void updatePosition(
            @Param("cardId") Long cardId,
            @Param("position") int position
    );

    @Modifying
    @Query(value = """
            UPDATE kanban_cards
            SET stage_id = :stageId,
                position = :position
            WHERE id = :cardId
            """, nativeQuery = true)
    void updateStageAndPosition(
            @Param("cardId") Long cardId,
            @Param("stageId") Long stageId,
            @Param("position") int position
    );

    @Modifying
    @Query(value = """
            UPDATE kanban_cards
            SET position = position + 1
            WHERE stage_id = :stageId AND position >= :position
            ORDER BY position DESC
            """, nativeQuery = true)
    void shiftPositionsFrom(
            @Param("stageId") Long stageId,
            @Param("position") int position
    );

    @Modifying
    @Query(value = """
            UPDATE kanban_cards
            SET position = position - 1
            WHERE stage_id = :stageId AND position > :position
            ORDER BY position ASC
            """, nativeQuery = true)
    void shiftPositionsAfterDelete(
            @Param("stageId") Long stageId,
            @Param("position") int position
    );

    @Modifying
    @Query(value = """
            UPDATE kanban_cards
            SET position = position + 1
            WHERE stage_id = :stageId
              AND position >= :newPosition
              AND position < :oldPosition
            ORDER BY position DESC
            """, nativeQuery = true)
    void shiftPositionsForMoveUp(
            @Param("stageId") Long stageId,
            @Param("oldPosition") int oldPosition,
            @Param("newPosition") int newPosition
    );

    @Modifying
    @Query(value = """
            UPDATE kanban_cards
            SET position = position - 1
            WHERE stage_id = :stageId
              AND position > :oldPosition
              AND position <= :newPosition
            ORDER BY position ASC
            """, nativeQuery = true)
    void shiftPositionsForMoveDown(
            @Param("stageId") Long stageId,
            @Param("oldPosition") int oldPosition,
            @Param("newPosition") int newPosition
    );

    // 사용자가 만든 카드 오름차 순으로 정렬 → 데드라인 정렬 사용
    @Query("""
            SELECT c FROM KanbanCard c
            JOIN FETCH c.jobPosting jp
            WHERE c.user.id = :userId
            ORDER BY jp.deadline ASC
            """)
    List<KanbanCard> findByUserIdOrderByJobPostingDeadlineAsc(@Param("userId") Long userId);

    // 마감 알림은 아직 지원 전(칸반 보드에서 가장 앞 스테이지)인 카드에만 보낸다.
    // 지원 완료/면접/최종 결과 등 다음 스테이지로 옮긴 카드는 대상에서 제외한다.
    @Query("""
            SELECT c FROM KanbanCard c
            JOIN FETCH c.user u
            JOIN FETCH c.jobPosting jp
            WHERE u.deletedAt IS NULL
              AND jp.deadline IN :deadlines
              AND c.stage.position = (
                  SELECT MIN(s.position) FROM KanbanStage s WHERE s.user = u
              )
            ORDER BY c.id ASC
            """)
    List<KanbanCard> findDeadlineReminderTargets(@Param("deadlines") List<LocalDate> deadlines);
}
