package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.NotNull;

public record KanbanCardMemoRequest(
        @NotNull
        String memo
) {
}
