package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.KanbanStage;

public record KanbanStageCreateResponse(
        Long id,
        String name,
        int position,
        boolean isDefault
) {
    public static KanbanStageCreateResponse from(KanbanStage stage) {
        return new KanbanStageCreateResponse(
                stage.getId(),
                stage.getStageName(),
                stage.getPosition(),
                stage.isDefault()
        );
    }
}
