package com.chwihap.server.domain.notification.entity;

import com.chwihap.server.domain.kanban.entity.KanbanCard;
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

    @Column(nullable = false, length = 500)
    private String message;

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
            boolean isRead,
            NotificationStatus status,
            LocalDateTime sentAt
    ) {
        this.user = user;
        this.kanbanCard = kanbanCard;
        this.type = type;
        this.message = message;
        this.isRead = isRead;
        this.status = status;
        this.sentAt = sentAt;
    }

    public static Notification email(
            User user,
            KanbanCard kanbanCard,
            String message,
            NotificationStatus status,
            LocalDateTime sentAt
    ) {
        Notification notification = Notification.builder()
                .user(user)
                .kanbanCard(kanbanCard)
                .type(NotificationType.EMAIL)
                .message(message)
                .isRead(true)
                .status(status)
                .sentAt(sentAt)
                .build();
        notification.createdAt = sentAt;
        return notification;
    }

    public static Notification inApp(
            User user,
            KanbanCard kanbanCard,
            String message,
            LocalDateTime createdAt
    ) {
        Notification notification = Notification.builder()
                .user(user)
                .kanbanCard(kanbanCard)
                .type(NotificationType.IN_APP)
                .message(message)
                .isRead(false)
                .status(NotificationStatus.SUCCESS)
                .sentAt(null)
                .build();
        notification.createdAt = createdAt;
        return notification;
    }
}
