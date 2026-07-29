package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 로켓펀치 Open API 응답을 {@link JobFeed}로 변환해 (platform=ROCKETPUNCH, externalId) 기준으로 upsert한다.
 * 페이지 순회는 트랜잭션 밖에서 수행하고, 페이지 단위로만 트랜잭션을 연다.
 * <p>
 * 로켓펀치 API는 채용공고에 지역/주소 필드를 전혀 제공하지 않는다({@code LocationResponse}는 이벤트 전용).
 * 따라서 region/regionRaw는 {@link RocketPunchCompanyRegionMapper}에 등록된 회사만 채워지고,
 * 그 외에는 null로 저장되어 지역 필터를 적용하면 노출되지 않는다.
 */
@Slf4j
@Service
public class JobFeedRocketPunchSyncService {

    private static final JobPlatform PLATFORM = JobPlatform.ROCKETPUNCH;
    private static final int COMPANY_MAX = 255;
    private static final int TITLE_MAX = 255;
    private static final int URL_MAX = 1000;
    private static final int THUMBNAIL_MAX = 500;
    private static final String SENIORITY_BEGINNER = "BEGINNER";

    private final JobFeedRepository jobFeedRepository;
    private final RocketPunchApiClient rocketPunchApiClient;
    private final RocketPunchCategoryMapper categoryMapper;
    private final RocketPunchCompanyRegionMapper companyRegionMapper;
    private final RocketPunchProperties properties;
    private final TransactionTemplate transactionTemplate;

    public JobFeedRocketPunchSyncService(JobFeedRepository jobFeedRepository,
                                         RocketPunchApiClient rocketPunchApiClient,
                                         RocketPunchCategoryMapper categoryMapper,
                                         RocketPunchCompanyRegionMapper companyRegionMapper,
                                         RocketPunchProperties properties,
                                         PlatformTransactionManager transactionManager) {
        this.jobFeedRepository = jobFeedRepository;
        this.rocketPunchApiClient = rocketPunchApiClient;
        this.categoryMapper = categoryMapper;
        this.companyRegionMapper = companyRegionMapper;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 채용공고를 앞쪽 N페이지까지 순회하며 job_feed에 upsert한다.
     * 한 페이지 호출이 재시도 후에도 실패하면 그 페이지만 건너뛰고 계속 진행한다.
     */
    public void sync() {
        if (!StringUtils.hasText(properties.apiKey())) {
            log.warn("로켓펀치 apiKey가 비어 있어 공고 수집을 건너뜁니다. (환경변수 ROCKETPUNCH_API_KEY 확인)");
            return;
        }

        int pageSize = properties.sync().pageSize();
        int maxPages = properties.sync().maxPages();
        int created = 0;
        int updated = 0;
        int skippedPages = 0;

        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            RocketPunchResponse response;
            try {
                response = rocketPunchApiClient.fetchPage(pageNo, pageSize);
            } catch (RocketPunchApiException e) {
                skippedPages++;
                log.warn("페이지 스킵 (pageNo={}): {}", pageNo, e.getMessage());
                continue;
            }

            List<RocketPunchResponse.Item> items = response == null ? null : response.items();
            if (items == null || items.isEmpty()) {
                break;
            }

            SyncResult result = transactionTemplate.execute(status -> upsertItems(items));
            if (result != null) {
                created += result.created();
                updated += result.updated();
            }

            boolean lastPage = items.size() < pageSize
                    || (response.totalPages() != null && pageNo >= response.totalPages());
            if (lastPage) {
                break;
            }
        }

        log.info("로켓펀치 공고 수집 완료: created={}, updated={}, skippedPages={}", created, updated, skippedPages);
    }

    private SyncResult upsertItems(List<RocketPunchResponse.Item> items) {
        List<RocketPunchResponse.Item> collectible = items.stream()
                .filter(this::isCollectible)
                .toList();
        if (collectible.isEmpty()) {
            return new SyncResult(0, 0);
        }

        List<String> externalIds = collectible.stream()
                .map(item -> String.valueOf(item.jobId()))
                .distinct()
                .toList();

        Map<String, JobFeed> existing = jobFeedRepository
                .findByPlatformAndExternalIdIn(PLATFORM, externalIds).stream()
                .collect(Collectors.toMap(JobFeed::getExternalId, Function.identity(), (a, b) -> a));

        int created = 0;
        int updated = 0;
        for (RocketPunchResponse.Item item : collectible) {
            String externalId = String.valueOf(item.jobId());
            String companyName = truncate(item.company().name(), COMPANY_MAX);
            String title = truncate(item.title(), TITLE_MAX);
            LocalDate deadline = parseDeadline(item.endAt());
            String thumbnailUrl = sanitizeUrl(item.company().logoUrl(), THUMBNAIL_MAX);
            String originalUrl = sanitizeUrl(item.webUrl(), URL_MAX);
            Set<CareerType> careerTypes = parseCareerTypes(item.seniorities());
            String category = categoryMapper.map(item.jobCategory());
            String region = companyRegionMapper.regionFor(item.company().name());

            JobFeed found = existing.get(externalId);
            if (found != null) {
                found.update(companyName, title, deadline, thumbnailUrl, originalUrl,
                        careerTypes, category, region, region);
                updated++;
            } else {
                jobFeedRepository.save(JobFeed.create(externalId, companyName, title, deadline,
                        thumbnailUrl, originalUrl, PLATFORM, careerTypes, category, region, region));
                created++;
            }
        }
        return new SyncResult(created, updated);
    }

    /**
     * jobId·회사명·제목이 있는 공고만 수집 대상. 목록 API는 진행중 공고만 반환하므로
     * 별도의 진행 여부 필드(hiring/status)는 목록 응답에 없다.
     */
    private boolean isCollectible(RocketPunchResponse.Item item) {
        return item.jobId() != null
                && item.company() != null
                && StringUtils.hasText(item.company().name())
                && StringUtils.hasText(item.title());
    }

    /**
     * seniorities 목록(BEGINNER/JUNIOR/MIDLEVEL/SENIOR/EXECUTIVE)을 신입/경력으로 단순화한다.
     * BEGINNER는 신입, 그 외는 경력으로 매핑하며 두 값이 섞여 있으면 양쪽 모두 담는다.
     */
    private Set<CareerType> parseCareerTypes(List<String> seniorities) {
        Set<CareerType> result = EnumSet.noneOf(CareerType.class);
        if (seniorities == null) {
            return result;
        }
        for (String seniority : seniorities) {
            if (SENIORITY_BEGINNER.equalsIgnoreCase(seniority)) {
                result.add(CareerType.NEW);
            } else if (StringUtils.hasText(seniority)) {
                result.add(CareerType.EXPERIENCED);
            }
        }
        return result;
    }

    /**
     * endAt은 ISO LocalDateTime 문자열("2026-08-19T15:00")이며, 상시채용이면 null이다.
     */
    private LocalDate parseDeadline(String endAt) {
        if (!StringUtils.hasText(endAt)) {
            return null;
        }
        try {
            return LocalDateTime.parse(endAt.trim()).toLocalDate();
        } catch (Exception e) {
            log.debug("마감일 파싱 실패, null 처리: {}", endAt);
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private String sanitizeUrl(String value, int max) {
        String truncated = truncate(value, max);
        if (truncated == null) {
            return null;
        }
        try {
            URI uri = new URI(truncated);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }
        return truncated;
    }

    private record SyncResult(int created, int updated) {
    }
}
