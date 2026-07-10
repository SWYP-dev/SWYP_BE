package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.NotNull;

public record KanbanCardMemoRequest(
        @NotNull(message = "memo는 필수입니다.")
        String memo
) {
}
