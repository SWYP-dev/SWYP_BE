package com.chwihap.server.domain.kanban.repository;

import com.chwihap.server.domain.kanban.entity.KanbanStage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KanbanStageRepository extends JpaRepository<KanbanStage, Long> {

    long countByUserIdAndIsDefaultFalse(Long userId);

    long countByUserId(Long userId);

    Optional<KanbanStage> findByUserIdAndId(Long userId, Long stageId);

    List<KanbanStage> findByUser_IdOrderByPositionAsc(Long userId);

    @Query("""
            SELECT COALESCE(MAX(s.position), 0)
            FROM KanbanStage s
            WHERE s.user.id = :userId
            """)
    int findMaxPositionByUserId(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM KanbanStage s WHERE s.id = :stageId")
    Optional<KanbanStage> lockById(@Param("stageId") Long stageId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE kanban_stages
            SET position = position + 1
            WHERE user_id = :userId AND position >= :position
            ORDER BY position DESC
            """, nativeQuery = true)
    void shiftPositionsFrom(@Param("userId") Long userId, @Param("position") int position);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE kanban_stages
            SET position = position + 1
            WHERE user_id = :userId
              AND position >= :newPosition
              AND position < :oldPosition
            ORDER BY position DESC
            """, nativeQuery = true)
    void shiftPositionsForMoveUp(
            @Param("userId") Long userId,
            @Param("oldPosition") int oldPosition,
            @Param("newPosition") int newPosition
    );

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE kanban_stages
            SET position = position - 1
            WHERE user_id = :userId
              AND position > :oldPosition
              AND position <= :newPosition
            ORDER BY position ASC
            """, nativeQuery = true)
    void shiftPositionsForMoveDown(
            @Param("userId") Long userId,
            @Param("oldPosition") int oldPosition,
            @Param("newPosition") int newPosition
    );

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE kanban_stages
            SET position = :position
            WHERE user_id = :userId AND id = :stageId
            """, nativeQuery = true)
    void updatePosition(
            @Param("userId") Long userId,
            @Param("stageId") Long stageId,
            @Param("position") int position
    );

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE kanban_stages
            SET stage_name = :stageName,
                position = :position
            WHERE user_id = :userId AND id = :stageId
            """, nativeQuery = true)
    void updateStageNameAndPosition(
            @Param("userId") Long userId,
            @Param("stageId") Long stageId,
            @Param("stageName") String stageName,
            @Param("position") int position
    );

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE kanban_stages
            SET position = position - 1
            WHERE user_id = :userId AND position > :position
            ORDER BY position ASC
            """, nativeQuery = true)
    void shiftPositionsAfterDelete(@Param("userId") Long userId, @Param("position") int position);

    List<KanbanStage> userId(Long userId);
}
