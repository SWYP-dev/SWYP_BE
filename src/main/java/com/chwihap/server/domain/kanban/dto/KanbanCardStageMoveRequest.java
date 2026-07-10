package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record KanbanCardStageMoveRequest(
        @NotNull(message = "stageId는 필수입니다.")
        Long stageId,

        @NotNull(message = "position은 필수입니다.")
        @Min(value = 1, message = "position은 1 이상이어야 합니다.")
        Integer position
) {
}
