package com.chwihap.server.domain.feed.dto;

public record FavoriteAddResponse(
        Long postingId,
        Long jobPostingId,
        boolean isFavorite
) {
}
