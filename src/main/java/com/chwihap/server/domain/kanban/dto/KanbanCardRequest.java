package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record KanbanCardRequest(
        @NotNull
        @Min(1)
        Long postingId
) {
}
