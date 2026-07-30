package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.feed.entity.JobPosting;
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
        JobPosting jobPosting = card.getJobPosting();
        KanbanStage stage = card.getStage();

        return new KanbanDeadlineCardResponse(
                card.getId(),
                jobPosting.getCompanyName(),
                jobPosting.getTitle(),
                jobPosting.getDeadline(),
                stage.getId(),
                stage.getStageName()
        );
    }
}
