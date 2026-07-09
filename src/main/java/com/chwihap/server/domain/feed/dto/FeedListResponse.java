package com.chwihap.server.domain.feed.dto;

import java.util.List;

public record FeedListResponse(
        List<FeedItemResponse> items,
        int page,
        int size,
        int totalPages,
        long totalElements,
        boolean hasNext
) {
}
