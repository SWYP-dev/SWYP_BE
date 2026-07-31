package com.chwihap.server.domain.kanban.dto;

import java.util.List;

public record KanbanDeadlineListResponse(
        List<KanbanDeadlineCardResponse> cards
) {
}
