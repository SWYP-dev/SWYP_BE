package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record KanbanCardResponse(
        Long Id,
        Long postingId,
        String companyName,
        String jobTitle,
        LocalDate deadline,
        String thumbnailUrl,
        String originalUrl,
        boolean deadlineChanged,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime registeredAt
) {
    public static KanbanCardResponse from(KanbanCard card) {
        JobPosting jobPosting = card.getJobPosting();

        return new KanbanCardResponse(
                card.getId(),
                jobPosting.getId(),
                jobPosting.getCompanyName(),
                jobPosting.getTitle(),
                jobPosting.getDeadline(),
                jobPosting.getThumbnailUrl(),
                jobPosting.getOriginalUrl(),
                card.isDeadlineChanged(),
                card.getCreatedAt()
        );
    }

}
