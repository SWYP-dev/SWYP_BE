package com.chwihap.server.domain.feed.dto;

import java.util.List;

public record FeedListResponse(
        List<FeedItemResponse> items,
        String nextCursor,
        boolean hasNext
) {
}
