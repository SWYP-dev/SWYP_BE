package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KanbanStageCreateRequest(
        @NotBlank @Size(max = 50) String name
) {
}
