package com.chwihap.server.domain.notification.repository;

import com.chwihap.server.domain.notification.entity.Notification;
import com.chwihap.server.domain.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.kanbanCard c
            JOIN FETCH c.jobPosting
            WHERE n.user.id = :userId
              AND n.type = :type
              AND (:cursor IS NULL OR n.id < :cursor)
            ORDER BY n.id DESC
            """)
    List<Notification> findHistory(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.kanbanCard c
            JOIN FETCH c.jobPosting
            WHERE n.user.id = :userId AND n.type = :type
            ORDER BY n.id DESC
            """)
    List<Notification> findInbox(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            Pageable pageable
    );

    long countByUser_IdAndTypeAndIsReadFalse(Long userId, NotificationType type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.user.id = :userId
              AND n.type = :type
              AND n.isRead = false
              AND n.id IN :ids
            """)
    int markAsRead(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("ids") List<Long> ids
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);

    boolean existsByUser_IdAndKanbanCard_IdAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            Long kanbanCardId,
            NotificationType type,
            LocalDateTime from,
            LocalDateTime to
    );
}
