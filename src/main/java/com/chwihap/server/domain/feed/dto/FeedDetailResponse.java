package com.chwihap.server.domain.feed.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeedDetailResponse(
        Long id,
        String platform,
        String companyName,
        String jobTitle,
        String jobCategory,
        String career,
        LocalDate deadline,
        String description,
        String thumbnailUrl,
        String originalUrl,
        boolean isFavorite,
        boolean isKanbanRegistered,
        LocalDateTime createdAt
) {
}
