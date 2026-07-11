package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record KanbanCardStageMoveRequest(
        @NotNull
        Long stageId,

        @NotNull
        @Min(1)
        Integer position
) {
}
