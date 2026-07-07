package com.chwihap.server.domain.feed.dto;

/**
 * 2.1 피드 목록 커서. sort=LATEST일 때는 id만, sort=DEADLINE일 때는 deadline까지 사용한다.
 * deadline이 null이면 "마감일 없는 공고" 구간(정렬상 맨 뒤)을 의미한다.
 */
public record FeedCursor(Long id, String deadline) {
}
