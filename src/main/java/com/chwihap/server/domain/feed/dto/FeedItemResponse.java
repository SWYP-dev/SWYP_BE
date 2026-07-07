package com.chwihap.server.domain.feed.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeedItemResponse(
        Long id,
        String platform,
        String companyName,
        String jobTitle,
        String jobCategory,
        String career,
        LocalDate deadline,
        String thumbnailUrl,
        String originalUrl,
        boolean isFavorite,
        boolean isExpired,
        LocalDateTime createdAt
) {
}
