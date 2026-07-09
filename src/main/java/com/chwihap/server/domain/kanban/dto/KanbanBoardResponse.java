package com.chwihap.server.domain.kanban.dto;

import java.util.List;

public record KanbanBoardResponse(
        List<KanbanStageResponse> stages
) {
}
