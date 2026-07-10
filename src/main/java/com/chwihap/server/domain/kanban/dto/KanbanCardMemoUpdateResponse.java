package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.KanbanCard;

public record KanbanCardMemoUpdateResponse(
        Long cardId,
        String memo
) {
    public static KanbanCardMemoUpdateResponse from(KanbanCard card) {
        return new KanbanCardMemoUpdateResponse(
                card.getId(),
                card.getMemo()
        );
    }
}
