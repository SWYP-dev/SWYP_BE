package com.chwihap.server.domain.feed.dto;

/**
 * 2.5 즐겨찾기 목록 커서. updatedAt(ISO_LOCAL_DATE_TIME)+id 기준으로 최신 즐겨찾기 순 정렬을 이어간다.
 */
public record FavoriteCursor(Long id, String updatedAt) {
}
