package com.chwihap.server.domain.feed.dto;

import com.chwihap.server.domain.feed.enums.JobPlatform;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code feedPage} 캐시에 저장하는 공고 항목. 유저별 스크랩 여부는 포함하지 않는다
 * (캐시가 여러 유저에게 공유되므로, 스크랩 여부는 캐시 조회 이후 매 요청 별도로 병합한다).
 */
public record CachedFeedItem(
        Long id,
        JobPlatform platform,
        String externalId,
        String companyName,
        String title,
        String category,
        List<String> careerTypes,
        String region,
        LocalDate deadline,
        String thumbnailUrl,
        String originalUrl,
        LocalDateTime crawledAt
) {
}
