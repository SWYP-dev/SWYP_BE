package com.chwihap.server.domain.feed.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ScrapListItemResponse(
        Long jobPostingId,
        String platform,
        String companyName,
        String jobTitle,
        String jobCategory,
        String career,
        String region,
        LocalDate deadline,
        String thumbnailUrl,
        String originalUrl,
        boolean isKanbanRegistered,
        LocalDateTime scrappedAt
) {
}
