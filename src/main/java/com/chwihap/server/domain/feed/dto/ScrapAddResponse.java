package com.chwihap.server.domain.feed.dto;

public record ScrapAddResponse(
        Long postingId,
        Long jobPostingId,
        boolean isScrapped
) {
}
