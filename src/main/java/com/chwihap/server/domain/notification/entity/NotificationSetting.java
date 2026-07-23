package com.chwihap.server.domain.notification.entity;

import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private boolean emailEnabled = true;

    @Column(nullable = false)
    private boolean inAppEnabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Integer> remindDays = new ArrayList<>(List.of(7, 3, 1, 0));

    @Builder(access = AccessLevel.PRIVATE)
    private NotificationSetting(
            User user,
            boolean emailEnabled,
            boolean inAppEnabled,
            List<Integer> remindDays
    ) {
        this.user = user;
        this.emailEnabled = emailEnabled;
        this.inAppEnabled = inAppEnabled;
        this.remindDays = new ArrayList<>(remindDays);
    }

    public static NotificationSetting createDefault(User user) {
        return NotificationSetting.builder()
                .user(user)
                .emailEnabled(true)
                .inAppEnabled(true)
                .remindDays(List.of(7, 3, 1, 0))
                .build();
    }

    public void update(boolean emailEnabled, boolean inAppEnabled, List<Integer> remindDays) {
        this.emailEnabled = emailEnabled;
        this.inAppEnabled = inAppEnabled;
        this.remindDays = new ArrayList<>(remindDays);
    }
}
