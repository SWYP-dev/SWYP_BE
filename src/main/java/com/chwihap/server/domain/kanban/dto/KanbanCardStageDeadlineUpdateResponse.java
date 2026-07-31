package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.KanbanStage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "지원 마감일 카드 수정 응답")
public record KanbanCardStageDeadlineUpdateResponse(
        @Schema(description = "수정된 카드 ID", example = "10")
        Long cardId,

        @Schema(description = "변경된 전형 단계 ID", example = "3")
        Long stageId,

        @Schema(description = "변경된 전형 단계 이름", example = "서류 지원")
        String stageName,

        @Schema(description = "변경된 전형 단계 내 카드 위치", example = "1")
        int position,

        @Schema(description = "변경된 지원 마감일", example = "2026-08-20")
        LocalDate deadline
) {
    public static KanbanCardStageDeadlineUpdateResponse of(
            Long cardId,
            KanbanStage stage,
            int position,
            LocalDate deadline
    ) {
        return new KanbanCardStageDeadlineUpdateResponse(
                cardId,
                stage.getId(),
                stage.getStageName(),
                position,
                deadline
        );
    }
}
