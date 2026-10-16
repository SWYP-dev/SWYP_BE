package com.chwihap.server.domain.notification.entity;

import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.notification.enums.NotificationReason;
import com.chwihap.server.domain.notification.enums.NotificationStatus;
import com.chwihap.server.domain.notification.enums.NotificationType;
import com.chwihap.server.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notifications_change_card_channel",
                columnNames = {"job_feed_change_id", "kanban_card_id", "type"}),
        indexes = {
                @Index(
                        name = "idx_notifications_user_id_is_read",
                        columnList = "user_id, is_read"
                ),
                @Index(
                        name = "idx_notifications_kanban_card_id",
                        columnList = "kanban_card_id"
                ),
                @Index(
                        name = "idx_notifications_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanban_card_id", nullable = false)
    private KanbanCard kanbanCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationReason reason = NotificationReason.DEADLINE_REMINDER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_feed_change_id", foreignKey = @ForeignKey(name = "fk_notifications_job_feed_change"))
    private JobFeedChange jobFeedChange;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 이 알림이 생성될 당시 마감일까지 남아 있던 일수.
     * 과거 알림을 나중에 조회하더라도 D-7/D-3/D-1/D-Day 표시가 바뀌지 않도록 스냅샷으로 보관한다.
     * 원본 공고 변경 알림에서는 사용하지 않으므로 null이다.
     */
    @Column(name = "days_before_deadline")
    private Integer daysBeforeDeadline;

    @Column(nullable = false)
    private boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = true)
    private LocalDateTime sentAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(
            User user,
            KanbanCard kanbanCard,
            NotificationType type,
            String message,
            Integer daysBeforeDeadline,
            boolean isRead,
            NotificationStatus status,
            LocalDateTime sentAt
    ) {
        this.user = user;
        this.kanbanCard = kanbanCard;
        this.type = type;
        this.message = message;
        this.daysBeforeDeadline = daysBeforeDeadline;
        this.isRead = isRead;
        this.status = status;
        this.sentAt = sentAt;
    }

    public static Notification email(
            User user,
            KanbanCard kanbanCard,
            String message,
            int daysBeforeDeadline,
            NotificationStatus status,
            LocalDateTime sentAt
    ) {
        Notification notification = Notification.builder()
                .user(user)
                .kanbanCard(kanbanCard)
                .type(NotificationType.EMAIL)
                .message(message)
                .daysBeforeDeadline(daysBeforeDeadline)
                .isRead(true)
                .status(status)
                .sentAt(sentAt)
                .build();
        return notification;
    }

    public static Notification inApp(
            User user,
            KanbanCard kanbanCard,
            String message,
            int daysBeforeDeadline
    ) {
        return Notification.builder()
                .user(user)
                .kanbanCard(kanbanCard)
                .type(NotificationType.IN_APP)
                .message(message)
                .daysBeforeDeadline(daysBeforeDeadline)
                .isRead(false)
                .status(NotificationStatus.SUCCESS)
                .sentAt(null)
                .build();
    }

    public static Notification jobFeedChanged(User user, KanbanCard card, JobFeedChange change,
                                               NotificationType channel, String message) {
        Notification notification = Notification.builder()
                .user(user).kanbanCard(card).type(channel).message(message)
                .daysBeforeDeadline(null).isRead(channel == NotificationType.EMAIL)
                .status(channel == NotificationType.IN_APP ? NotificationStatus.SUCCESS : NotificationStatus.PENDING)
                .build();
        notification.reason = NotificationReason.JOB_FEED_CHANGED;
        notification.jobFeedChange = change;
        return notification;
    }

    public void completeEmail(NotificationStatus status, LocalDateTime attemptedAt) {
        this.status = status;
        this.sentAt = attemptedAt;
    }
}
