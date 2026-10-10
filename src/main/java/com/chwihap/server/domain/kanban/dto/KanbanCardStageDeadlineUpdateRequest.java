package com.chwihap.server.domain.kanban.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;

@Schema(description = "지원 마감일 카드 수정 요청")
public record KanbanCardStageDeadlineUpdateRequest(
        @Schema(description = "변경할 전형 단계 ID. 생략하면 기존 단계를 유지", example = "3")
        Long stageId,

        @Schema(description = "변경할 지원 마감일. 생략하면 기존 마감일을 유지하고, null을 전달하면 상시채용(마감일 없음)으로 변경", example = "2026-08-20")
        JsonNullable<LocalDate> deadline
) {
    public KanbanCardStageDeadlineUpdateRequest {
        if (deadline == null) {
            deadline = JsonNullable.undefined();
        }
    }
}
