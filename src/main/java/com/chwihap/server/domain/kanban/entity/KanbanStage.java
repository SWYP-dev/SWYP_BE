package com.chwihap.server.domain.kanban.entity;

import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "kanban_stages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kanban_stages_user_position",
                        columnNames = {"user_id", "position"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KanbanStage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String stageName;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean isDefault = false;

}
