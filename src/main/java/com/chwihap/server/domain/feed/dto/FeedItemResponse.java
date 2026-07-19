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
        String region,
        LocalDate deadline,
        String thumbnailUrl,
        String originalUrl,
        boolean isScrapped,
        boolean isExpired,
        LocalDateTime createdAt
) {
}
