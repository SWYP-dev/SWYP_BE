package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.NotNull;

public record KanbanCardRequest(
        @NotNull(message = "postingId는 필수입니다.")
        Long postingId
) {
}
