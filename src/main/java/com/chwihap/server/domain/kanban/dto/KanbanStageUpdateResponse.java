package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.KanbanStage;

public record KanbanStageUpdateResponse(
        Long id,
        String name,
        int position
) {
    public static KanbanStageUpdateResponse from(KanbanStage stage) {
        return new KanbanStageUpdateResponse(
                stage.getId(),
                stage.getStageName(),
                stage.getPosition()
        );
    }
}
