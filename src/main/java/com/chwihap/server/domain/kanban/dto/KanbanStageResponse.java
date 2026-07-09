package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.KanbanStage;

import java.util.List;

public record KanbanStageResponse(
        Long Id,
        String name,
        int position,
        boolean isDefault,
        List<KanbanCardResponse> cards
) {
    public static KanbanStageResponse from(
        KanbanStage stage,
        List<KanbanCardResponse> cards
    ) {
        return new KanbanStageResponse(
                stage.getId(),
                stage.getStageName(),
                stage.getPosition(),
                stage.isDefault(),
                cards
        );
    }

}
