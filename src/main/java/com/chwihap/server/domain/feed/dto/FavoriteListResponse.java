package com.chwihap.server.domain.feed.dto;

import java.util.List;

public record FavoriteListResponse(
        List<FavoriteListItemResponse> items,
        String nextCursor,
        boolean hasNext
) {
}
