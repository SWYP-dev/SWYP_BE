package com.chwihap.server.domain.kanban.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "지원 마감일 카드 수정 요청")
public record KanbanCardStageDeadlineUpdateRequest(
        @Schema(description = "변경할 전형 단계 ID. 생략하면 기존 단계를 유지", example = "3")
        @JsonSetter(nulls = Nulls.FAIL)
        Long stageId,

        @Schema(description = "변경할 지원 마감일. 생략하면 기존 마감일을 유지", example = "2026-08-20")
        @JsonSetter(nulls = Nulls.FAIL)
        LocalDate deadline
) {
}
