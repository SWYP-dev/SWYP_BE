package com.chwihap.server.domain.feed.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FavoriteListItemResponse(
        Long jobPostingId,
        String companyName,
        String jobTitle,
        LocalDate deadline,
        String thumbnailUrl,
        String originalUrl,
        boolean isKanbanRegistered,
        LocalDateTime favoritedAt
) {
}
