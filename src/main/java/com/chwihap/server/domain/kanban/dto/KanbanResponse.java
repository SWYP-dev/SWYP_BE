package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.KanbanStage;

public record KanbanResponse(
        Long id,
        String name,
        int position,
        boolean isDefault
) {
    public static KanbanResponse from(KanbanStage stage) {
        return new KanbanResponse(
                stage.getId(),
                stage.getStageName(),
                stage.getPosition(),
                stage.isDefault()
        );
    }
}
