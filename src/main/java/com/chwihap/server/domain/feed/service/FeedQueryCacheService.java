package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.feed.dto.CachedFeedItem;
import com.chwihap.server.domain.feed.dto.CachedFeedPage;
import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.FeedSort;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 공고 피드 DB 조회 결과를 Redis에 캐싱하는 전담 서비스.
 * FeedService가 자기 자신의 @Cacheable 메서드를 호출하면(self-invocation) AOP 프록시를 거치지 않아
 * 캐싱이 동작하지 않으므로, 캐싱 대상 조회 로직을 별도 빈으로 분리했다.
 * 유저별 스크랩 상태는 여기서 다루지 않는다 — 캐시는 여러 유저에게 공유되기 때문에,
 * 스크랩 병합은 반드시 이 메서드 호출 이후 FeedService에서 매 요청마다 수행해야 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedQueryCacheService {

    private static final List<JobPlatform> PUBLIC_SECTOR_PLATFORMS = Arrays.stream(JobPlatform.values())
            .filter(JobPlatform::isPublicSector)
            .toList();

    private final JobFeedRepository jobFeedRepository;

    /**
     * 필터 없는 기본(baseline) 조회일 때만 실제로 Redis 캐시를 사용한다.
     * 필터가 하나라도 걸리면 condition이 false가 되어 매번 리포지토리를 직접 호출한다
     * (필터 조회는 결과 row 수가 적어 이미 빠르고, 캐시 키 카디널리티 폭증도 방지한다).
     */
    @Cacheable(cacheNames = "feedPage", condition =
            "#platforms.size() == T(com.chwihap.server.domain.feed.enums.JobPlatform).values().length "
                    + "and !#hasCategoryFilter and !#hasCareerFilter and !#hasRegionFilter "
                    + "and !#deadlineSoon and #keyword == null")
    public CachedFeedPage getFeedPage(FeedSort sort, List<JobPlatform> platforms,
                                       boolean hasCategoryFilter, List<String> categories,
                                       boolean hasCareerFilter, List<CareerType> careers,
                                       boolean hasRegionFilter, List<String> regions,
                                       boolean deadlineSoon, LocalDate today, LocalDate soonUntil,
                                       boolean excludeExpired, String keyword, Pageable pageable) {
        Page<JobFeed> result = sort == FeedSort.DEADLINE
                ? jobFeedRepository.findDeadlinePage(platforms,
                        hasCategoryFilter, categories, hasCareerFilter, careers, hasRegionFilter, regions,
                        deadlineSoon, today, soonUntil, excludeExpired, keyword, pageable)
                : jobFeedRepository.findLatestPage(platforms,
                        hasCategoryFilter, categories, hasCareerFilter, careers, hasRegionFilter, regions,
                        deadlineSoon, today, soonUntil, excludeExpired, keyword, PUBLIC_SECTOR_PLATFORMS, pageable);

        return toCachedFeedPage(result);
    }

    /**
     * JobFeed는 careerTypes가 LAZY 컬렉션이라 트랜잭션 밖(캐시 역직렬화 시점)에서 접근하면 위험하다.
     * 트랜잭션이 열려있는 지금 순수 DTO로 매핑해서, 캐시에는 DTO만 저장되도록 한다.
     */
    private CachedFeedPage toCachedFeedPage(Page<JobFeed> page) {
        List<CachedFeedItem> items = page.getContent().stream()
                .map(this::toCachedFeedItem)
                .collect(Collectors.toList());
        return new CachedFeedPage(items, page.getNumber(), page.getSize(),
                page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    private CachedFeedItem toCachedFeedItem(JobFeed feed) {
        List<String> careerTypes = feed.getCareerTypes().stream()
                .sorted()
                .map(CareerType::name)
                .collect(Collectors.toList());
        return new CachedFeedItem(
                feed.getId(),
                feed.getPlatform(),
                feed.getExternalId(),
                feed.getCompanyName(),
                feed.getTitle(),
                feed.getCategory(),
                careerTypes,
                feed.getRegion(),
                feed.getDeadline(),
                feed.getThumbnailUrl(),
                feed.getOriginalUrl(),
                feed.getCrawledAt()
        );
    }
}
