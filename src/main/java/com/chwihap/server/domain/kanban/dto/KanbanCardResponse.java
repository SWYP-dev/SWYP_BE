package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record KanbanCardResponse(
        Long id,
        Long postingId,
        String companyName,
        String jobTitle,
        LocalDate deadline,
        String thumbnailUrl,
        String originalUrl,
        boolean deadlineChanged,
        String memo,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime registeredAt
) {
    public static KanbanCardResponse from(KanbanCard card) {
        ApplicationPosting applicationPosting = card.getApplicationPosting();

        return new KanbanCardResponse(
                card.getId(),
                applicationPosting.getId(),
                applicationPosting.getCompanyName(),
                applicationPosting.getTitle(),
                applicationPosting.getDeadline(),
                applicationPosting.getThumbnailUrl(),
                applicationPosting.getOriginalUrl(),
                card.isDeadlineChanged(),
                card.getMemo(),
                card.getCreatedAt()
        );
    }

}
