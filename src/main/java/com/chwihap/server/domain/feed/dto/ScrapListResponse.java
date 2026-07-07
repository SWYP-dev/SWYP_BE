package com.chwihap.server.domain.feed.dto;

import java.util.List;

public record ScrapListResponse(
        List<ScrapListItemResponse> items,
        int page,
        int size,
        int totalPages,
        long totalElements,
        boolean hasNext
) {
}
