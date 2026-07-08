package com.chwihap.server.domain.feed.dto;

public record ScrapRemoveResponse(
        Long jobPostingId,
        boolean isScrapped
) {
}
