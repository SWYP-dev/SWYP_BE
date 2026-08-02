package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;

import java.time.LocalDate;

public record KanbanCardCreateResponse(
        Long cardId,
        Long stageId,
        String stageName,
        Long postingId,
        String companyName,
        String jobTitle,
        LocalDate deadline
) {
    public static KanbanCardCreateResponse from(KanbanCard card) {
        KanbanStage stage = card.getStage();
        ApplicationPosting applicationPosting = card.getApplicationPosting();

        return new KanbanCardCreateResponse(
                card.getId(),
                stage.getId(),
                stage.getStageName(),
                applicationPosting.getId(),
                applicationPosting.getCompanyName(),
                applicationPosting.getTitle(),
                applicationPosting.getDeadline()
        );
    }
}
