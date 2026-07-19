package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.enums.Region;
import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * data.go.kr 채용정보 API 응답을 {@link JobFeed}로 변환해 (platform=PUBLIC, externalId) 기준으로 upsert한다.
 * 페이지 순회는 트랜잭션 밖에서 수행하고(HTTP 지연이 DB 커넥션을 점유하지 않도록), 페이지 단위로만 트랜잭션을 연다.
 */
@Slf4j
@Service
public class JobFeedSyncService {

    private static final JobPlatform PLATFORM = JobPlatform.PUBLIC;
    private static final String ONGOING = "Y";
    private static final int COMPANY_MAX = 255;
    private static final int TITLE_MAX = 255;
    private static final int CATEGORY_MAX = 255;
    private static final int REGION_MAX = 100;
    private static final int REGION_RAW_MAX = 500;
    private static final int URL_MAX = 1000;
    private static final DateTimeFormatter[] DEADLINE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    private final JobFeedRepository jobFeedRepository;
    private final RecruitmentApiClient recruitmentApiClient;
    private final DataGoKrProperties properties;
    private final TransactionTemplate transactionTemplate;

    public JobFeedSyncService(JobFeedRepository jobFeedRepository,
                              RecruitmentApiClient recruitmentApiClient,
                              DataGoKrProperties properties,
                              PlatformTransactionManager transactionManager) {
        this.jobFeedRepository = jobFeedRepository;
        this.recruitmentApiClient = recruitmentApiClient;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 진행중 공고를 앞쪽 N페이지까지 순회하며 job_feed에 upsert한다.
     * 한 페이지 호출이 재시도 후에도 실패하면 그 페이지만 건너뛰고 계속 진행한다.
     */
    public void sync() {
        if (!StringUtils.hasText(properties.serviceKey())) {
            log.warn("data.go.kr serviceKey가 비어 있어 공고 수집을 건너뜁니다. (환경변수 DATA_GO_KR_SERVICE_KEY 확인)");
            return;
        }

        int numOfRows = properties.sync().numOfRows();
        int maxPages = properties.sync().maxPages();
        int created = 0;
        int updated = 0;
        int skippedPages = 0;

        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            RecruitmentResponse response;
            try {
                response = recruitmentApiClient.fetchPage(pageNo, numOfRows);
            } catch (RecruitmentApiException e) {
                skippedPages++;
                log.warn("페이지 스킵 (pageNo={}): {}", pageNo, e.getMessage());
                continue;
            }

            List<RecruitmentResponse.Item> items = response == null ? null : response.result();
            if (items == null || items.isEmpty()) {
                break;
            }

            SyncResult result = transactionTemplate.execute(status -> upsertItems(items));
            if (result != null) {
                created += result.created();
                updated += result.updated();
            }

            if (items.size() < numOfRows) {
                break; // 마지막 페이지
            }
        }

        log.info("공고 수집 완료: created={}, updated={}, skippedPages={}", created, updated, skippedPages);
    }

    private SyncResult upsertItems(List<RecruitmentResponse.Item> items) {
        List<RecruitmentResponse.Item> collectible = items.stream()
                .filter(this::isCollectible)
                .toList();
        if (collectible.isEmpty()) {
            return new SyncResult(0, 0);
        }

        List<String> externalIds = collectible.stream()
                .map(item -> String.valueOf(item.recrutPblntSn()))
                .distinct()
                .toList();

        Map<String, JobFeed> existing = jobFeedRepository
                .findByPlatformAndExternalIdIn(PLATFORM, externalIds).stream()
                .collect(Collectors.toMap(JobFeed::getExternalId, Function.identity(), (a, b) -> a));

        int created = 0;
        int updated = 0;
        for (RecruitmentResponse.Item item : collectible) {
            String externalId = String.valueOf(item.recrutPblntSn());
            JobFeed found = existing.get(externalId);
            if (found != null) {
                found.update(
                        truncate(item.instNm(), COMPANY_MAX),
                        truncate(item.recrutPbancTtl(), TITLE_MAX),
                        parseDeadline(item.pbancEndYmd()),
                        null,
                        sanitizeUrl(item.srcUrl()),
                        parseCareerTypes(item.recrutSeNm()),
                        truncate(item.ncsCdNmLst(), CATEGORY_MAX),
                        firstRegion(item.workRgnNmLst()),
                        truncate(item.workRgnNmLst(), REGION_RAW_MAX));
                updated++;
            } else {
                jobFeedRepository.save(JobFeed.create(
                        externalId,
                        truncate(item.instNm(), COMPANY_MAX),
                        truncate(item.recrutPbancTtl(), TITLE_MAX),
                        parseDeadline(item.pbancEndYmd()),
                        null,
                        sanitizeUrl(item.srcUrl()),
                        PLATFORM,
                        parseCareerTypes(item.recrutSeNm()),
                        truncate(item.ncsCdNmLst(), CATEGORY_MAX),
                        firstRegion(item.workRgnNmLst()),
                        truncate(item.workRgnNmLst(), REGION_RAW_MAX)));
                created++;
            }
        }
        return new SyncResult(created, updated);
    }

    /**
     * 진행중이면서 필수 필드(일련번호·기관명·제목)가 있는 공고만 수집 대상.
     */
    private boolean isCollectible(RecruitmentResponse.Item item) {
        return item.recrutPblntSn() != null
                && ONGOING.equalsIgnoreCase(item.ongoingYn())
                && StringUtils.hasText(item.instNm())
                && StringUtils.hasText(item.recrutPbancTtl());
    }

    /**
     * recrutSeNm 원문("신입" / "경력" / "신입+경력" 등)에서 채용구분을 추출한다.
     * "신입+경력"은 두 값을 모두 담아 양쪽 필터에 노출되도록 한다.
     */
    private Set<CareerType> parseCareerTypes(String recrutSeNm) {
        Set<CareerType> result = EnumSet.noneOf(CareerType.class);
        if (!StringUtils.hasText(recrutSeNm)) {
            return result;
        }
        if (recrutSeNm.contains("신입")) {
            result.add(CareerType.NEW);
        }
        if (recrutSeNm.contains("경력")) {
            result.add(CareerType.EXPERIENCED);
        }
        return result;
    }

    private LocalDate parseDeadline(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        for (DateTimeFormatter format : DEADLINE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (Exception ignored) {
                // 다음 포맷 시도
            }
        }
        log.debug("마감일 파싱 실패, null 처리: {}", raw);
        return null;
    }

    /**
     * 다지역 원문에서 첫 지역명을 표준 시/도 라벨로 정규화한다.
     * 17개 시/도 목록에 없는 지역명(시/군/구·지역구 단위 등)은 {@link Region#OTHER}("기타")로 묶는다.
     */
    private String firstRegion(String workRgnNmLst) {
        if (!StringUtils.hasText(workRgnNmLst)) {
            return null;
        }
        String first = workRgnNmLst.split(",")[0].trim();
        return first.isEmpty() ? null : truncate(Region.fromRaw(first).getLabel(), REGION_MAX);
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    /**
     * 원본 API의 srcUrl은 "없음." 같은 비-URL 문자열이 섞여 오는 경우가 있어,
     * http(s) 스킴을 갖춘 값만 통과시키고 그 외엔 null로 저장한다.
     */
    private String sanitizeUrl(String value) {
        String truncated = truncate(value, URL_MAX);
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
