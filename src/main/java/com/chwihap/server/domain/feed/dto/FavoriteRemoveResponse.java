package com.chwihap.server.domain.feed.dto;

public record FavoriteRemoveResponse(
        Long jobPostingId,
        boolean isFavorite
) {
}
