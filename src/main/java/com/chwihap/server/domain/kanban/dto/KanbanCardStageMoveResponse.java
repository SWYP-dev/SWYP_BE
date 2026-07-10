package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.KanbanStage;

public record KanbanCardStageMoveResponse(
        Long cardId,
        Long stageId,
        String stageName,
        int position
) {
    public static KanbanCardStageMoveResponse of(Long cardId, KanbanStage stage, int position) {
        return new KanbanCardStageMoveResponse(
                cardId,
                stage.getId(),
                stage.getStageName(),
                position
        );
    }
}
