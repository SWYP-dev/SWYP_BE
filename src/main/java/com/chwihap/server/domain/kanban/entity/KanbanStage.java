package com.chwihap.server.domain.kanban.entity;

import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "kanban_stages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kanban_stages_user_position",
                        columnNames = {"user_id", "position"}
                ),
                @UniqueConstraint(
                        name = "uk_kanban_stages_user_name",
                        columnNames = {"user_id", "stage_name"}
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

    @Builder(access = AccessLevel.PRIVATE)
    private KanbanStage(User user, String stageName, int position, boolean isDefault) {
        this.user = user;
        this.stageName = stageName;
        this.position = position;
        this.isDefault = isDefault;
    }

    /**
     * 칸반보드 커스텀을 하지 않는 경우 사용하는 메소드
     * @param user
     * @param stageName
     * @param position
     * @return 칸반보드 디폴트 객체
     *
     * @author say_0
     */
    public static KanbanStage kanbanDefault(User user, String stageName, int position) {
        return KanbanStage.builder()
                .user(user)
                .stageName(stageName)
                .position(position)
                .isDefault(true)
                .build();
    }

    /**
     * 칸반보드 커스텀을 하는 경우 사용하는 메소드
     * @param user
     * @param stageName
     * @param position
     * @return 칸반보드 커스텀 객체
     *
     * @author say_0
     */
    public static KanbanStage createCustom(User user, String stageName, int position) {
        return KanbanStage.builder()
                .user(user)
                .stageName(stageName)
                .position(position)
                .isDefault(false)
                .build();
    }

    /**
     * 칸반보드 커스텀 스테이지 변경 메소드
     * @param stageName
     * @param position
     *
     * @author say_0
     */
    public void updateStage(String stageName, int position) {
        this.stageName = stageName;
        this.position = position;
    }

}
