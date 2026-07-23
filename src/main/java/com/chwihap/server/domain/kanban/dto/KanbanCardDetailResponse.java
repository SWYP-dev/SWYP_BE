package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record KanbanCardDetailResponse(
        Long id,
        Long postingId,
        String companyName,
        String jobTitle,
        String thumbnailUrl,
        String jobCategory,
        String region,
        CareerType career,
        LocalDate deadline,
        String originalUrl,
        boolean deadlineChanged,
        String memo,
        List<KanbanCardDocumentResponse> documents,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime registeredAt
) {
    public static KanbanCardDetailResponse from(KanbanCard card, List<Document> documents) {
        JobPosting jobPosting = card.getJobPosting();

        return new KanbanCardDetailResponse(
                card.getId(),
                jobPosting.getId(),
                jobPosting.getCompanyName(),
                jobPosting.getTitle(),
                jobPosting.getThumbnailUrl(),
                jobPosting.getCategory(),
                jobPosting.getRegion(),
                jobPosting.getCareerType(),
                jobPosting.getDeadline(),
                jobPosting.getOriginalUrl(),
                card.isDeadlineChanged(),
                card.getMemo(),
                documents.stream()
                        .map(KanbanCardDocumentResponse::from)
                        .toList(),
                card.getCreatedAt()
        );
    }
}
