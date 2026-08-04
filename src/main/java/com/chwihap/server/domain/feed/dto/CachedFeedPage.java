package com.chwihap.server.domain.feed.dto;

import java.util.List;

/**
 * {@code feedPage} 캐시에 저장하는 페이지 단위 조회 결과 (DB 조회 결과만 담고, 유저별 스크랩 상태는 병합 전 상태).
 */
public record CachedFeedPage(
        List<CachedFeedItem> items,
        int number,
        int size,
        int totalPages,
        long totalElements,
        boolean hasNext
) {
}
