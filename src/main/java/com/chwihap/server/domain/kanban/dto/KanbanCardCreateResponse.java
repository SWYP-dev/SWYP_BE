package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.feed.entity.JobPosting;
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
        JobPosting jobPosting = card.getJobPosting();

        return new KanbanCardCreateResponse(
                card.getId(),
                stage.getId(),
                stage.getStageName(),
                jobPosting.getId(),
                jobPosting.getCompanyName(),
                jobPosting.getTitle(),
                jobPosting.getDeadline()
        );
    }
}
