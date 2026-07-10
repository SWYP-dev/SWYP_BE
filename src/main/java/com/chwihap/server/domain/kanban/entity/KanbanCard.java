package com.chwihap.server.domain.kanban.entity;

import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "kanban_cards",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kanban_cards_user_job_posting",
                        columnNames = {"user_id", "job_posting_id"}
                ),
                @UniqueConstraint(
                        name = "uk_kanban_cards_stage_position",
                        columnNames = {"stage_id", "position"}
                ),
                @UniqueConstraint(
                        name = "uk_kanban_cards_user_deadline_position",
                        columnNames = {"user_id", "deadline_position"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KanbanCard extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private KanbanStage stage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false, unique = true)
    private JobPosting jobPosting;

    @Column(nullable = false)
    private int position;

    @Column(nullable = true)
    private Integer deadlinePosition;

    @Column(nullable = false)
    private boolean deadlineChanged = false;

    @Column(columnDefinition = "text")
    private String memo;

    @Builder(access = AccessLevel.PRIVATE)
    private KanbanCard(
            User user,
            KanbanStage stage,
            JobPosting jobPosting,
            int position,
            Integer deadlinePosition,
            boolean deadlineChanged
    ) {
        this.user = user;
        this.stage = stage;
        this.jobPosting = jobPosting;
        this.position = position;
        this.deadlinePosition = deadlinePosition;
        this.deadlineChanged = deadlineChanged;
    }

    public static KanbanCard createCard(User user, KanbanStage stage, JobPosting jobPosting, int position) {
        return KanbanCard.builder()
                .user(user)
                .stage(stage)
                .jobPosting(jobPosting)
                .position(position)
                .build();
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

}
