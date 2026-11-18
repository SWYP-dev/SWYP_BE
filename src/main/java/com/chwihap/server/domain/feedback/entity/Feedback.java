package com.chwihap.server.domain.feedback.entity;

import com.chwihap.server.domain.feedback.enums.FeedbackType;
import com.chwihap.server.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 비로그인 접수 시 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_feedback_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // SERVICE_INQUIRY일 때만 저장
    @Column(length = 254)
    private String email;

    @Column(name = "image_key", length = 500)
    private String imageKey;

    // 접수 후 수정하지 않으므로 updated_at 없이 접수 시각만 기록
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Feedback create(User user, FeedbackType type, String content, String email, String imageKey) {
        Feedback feedback = new Feedback();
        feedback.user = user;
        feedback.type = type;
        feedback.content = content;
        feedback.email = email;
        feedback.imageKey = imageKey;
        return feedback;
    }
}
