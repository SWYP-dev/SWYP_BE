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

    boolean existsByUser_IdAndApplicationPosting_SourceJobPosting_Id(
            Long userId,
            Long sourceJobPostingId
    );

    Optional<KanbanCard> findByIdAndUser_Id(Long cardId, Long userId);

    long countByStage(KanbanStage stage);

    @Query("""
            SELECT c FROM KanbanCard c
            JOIN FETCH c.applicationPosting
            WHERE c.user.id = :userId
            ORDER BY c.position ASC
            """)
    List<KanbanCard> findByUser_IdOrderByPositionAsc(@Param("userId") Long userId);

    boolean existsByApplicationPosting_Id(Long applicationPostingId);

    boolean existsByUser_IdAndApplicationPosting_OriginalUrl(Long userId, String url);

    boolean existsByUser_IdAndApplicationPosting_OriginalUrlAndIdNot(Long userId, String url, Long cardId);

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

    // 지원 마감일 페이지: 오늘 이후 마감되는 사용자의 전체 칸반 카드를 마감일 순으로 조회
    @Query("""
            SELECT c FROM KanbanCard c
            JOIN FETCH c.applicationPosting ap
            JOIN FETCH c.stage
            WHERE c.user.id = :userId
              AND ap.deadline >= :today
            ORDER BY ap.deadline ASC, c.id ASC
            """)
    List<KanbanCard> findUpcomingDeadlineCards(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    // 마감 알림은 칸반 순서와 관계없이 기본 '지원 전' 스테이지의 카드에만 보낸다.
    // 지원 완료/면접/최종 결과 등 다른 스테이지로 옮긴 카드는 대상에서 제외한다.
    @Query("""
            SELECT c FROM KanbanCard c
            JOIN FETCH c.user u
            JOIN FETCH c.applicationPosting ap
            WHERE u.deletedAt IS NULL
              AND ap.deadline IN :deadlines
              AND c.stage.isDefault = true
              AND c.stage.stageName = :stageName
            ORDER BY c.id ASC
            """)
    List<KanbanCard> findDeadlineReminderTargets(
            @Param("deadlines") List<LocalDate> deadlines,
            @Param("stageName") String stageName
    );
}
