package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KanbanStageRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(1) Integer position
) {
}
