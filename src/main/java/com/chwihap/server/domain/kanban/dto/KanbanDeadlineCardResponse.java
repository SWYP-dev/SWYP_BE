package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;

import java.time.LocalDate;

public record KanbanDeadlineCardResponse(
        Long cardId,
        String companyName,
        String jobTitle,
        LocalDate deadline,
        Long stageId,
        String stageName
) {
    public static KanbanDeadlineCardResponse from(KanbanCard card) {
        ApplicationPosting applicationPosting = card.getApplicationPosting();
        KanbanStage stage = card.getStage();

        return new KanbanDeadlineCardResponse(
                card.getId(),
                applicationPosting.getCompanyName(),
                applicationPosting.getTitle(),
                applicationPosting.getDeadline(),
                stage.getId(),
                stage.getStageName()
        );
    }
}
