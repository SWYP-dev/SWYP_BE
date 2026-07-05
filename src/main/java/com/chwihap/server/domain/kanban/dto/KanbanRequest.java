package com.chwihap.server.domain.kanban.dto;

public record KanbanRequest(
        String name,
        int position
) {
}
