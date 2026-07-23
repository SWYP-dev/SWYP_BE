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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 인사혁신처 공공취업정보 API 응답을 {@link JobFeed}로 변환해
 * (platform=PUBLIC_PERSONNEL, externalId) 기준으로 upsert한다.
 * 페이지 순회는 트랜잭션 밖에서 수행하고(HTTP 지연이 DB 커넥션을 점유하지 않도록), 페이지 단위로만 트랜잭션을 연다.
 */
@Slf4j
@Service
public class JobFeedPersonnelJobSyncService {

    private static final JobPlatform PLATFORM = JobPlatform.PUBLIC_PERSONNEL;
    private static final String CAREER_TYPE_EXPERIENCED_CODE = "e02";
    private static final int COMPANY_MAX = 255;
    private static final int TITLE_MAX = 255;
    private static final int REGION_MAX = 100;
    private static final int REGION_RAW_MAX = 500;
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /**
     * 나라일터(gojobs.go.kr) 상세페이지 URL 패턴. idx(=empmnsn)만 있으면 원문 공고로 연결된다.
     */
    private static final String ORIGINAL_URL_TEMPLATE = "https://www.gojobs.go.kr/apmView.do?empmnsn=%s";

    /**
     * 공고유형(type01) 코드 → 한글 라벨. 이 API는 NCS 직무 분류가 없어 category 대체용으로 사용한다.
     */
    private static final Map<String, String> ANNOUNCEMENT_TYPE_LABELS = Map.of(
            "e01", "공개경쟁채용",
            "e02", "경력경쟁채용",
            "e03", "계약직",
            "e04", "행정지원인력",
            "e06", "공모직위"
    );

    /**
     * 원본 API가 종료일 미상(상시채용 등)인 공고에 실제 마감일 대신 내려주는 sentinel 값.
     * 이런 공고는 category를 "상시채용"으로 덮어써서 실제 채용유형 라벨 대신 노출한다.
     */
    private static final String NO_DEADLINE_SENTINEL = "99991231";
    private static final String ONGOING_RECRUITMENT_LABEL = "상시채용";

    /**
     * 채용공고가 아니라 결과 발표문(최종합격자 등)인 title은 수집 대상에서 제외한다.
     * enddate가 비어 sentinel로 잡히면서 함께 섞여 들어오는 경우가 있었다.
     */
    private static final List<String> RESULT_ANNOUNCEMENT_TITLE_KEYWORDS = List.of(
            "최종합격자", "합격자 발표", "합격자발표"
    );

    private final JobFeedRepository jobFeedRepository;
    private final PersonnelJobApiClient personnelJobApiClient;
    private final PersonnelJobProperties properties;
    private final TransactionTemplate transactionTemplate;

    public JobFeedPersonnelJobSyncService(JobFeedRepository jobFeedRepository,
                                          PersonnelJobApiClient personnelJobApiClient,
                                          PersonnelJobProperties properties,
                                          PlatformTransactionManager transactionManager) {
        this.jobFeedRepository = jobFeedRepository;
        this.personnelJobApiClient = personnelJobApiClient;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 공고를 앞쪽 N페이지까지 순회하며 job_feed에 upsert한다.
     * 한 페이지 호출이 재시도 후에도 실패하면 그 페이지만 건너뛰고 계속 진행한다.
     */
    public void sync() {
        if (!StringUtils.hasText(properties.serviceKey())) {
            log.warn("공공취업정보 serviceKey가 비어 있어 공고 수집을 건너뜁니다. (환경변수 DATA_GO_KR_PERSONNEL_SERVICE_KEY 확인)");
            return;
        }

        int numOfRows = properties.sync().numOfRows();
        int maxPages = properties.sync().maxPages();
        int created = 0;
        int updated = 0;
        int skippedPages = 0;

        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            PersonnelJobResponse response;
            try {
                response = personnelJobApiClient.fetchPage(pageNo, numOfRows);
            } catch (PersonnelJobApiException e) {
                skippedPages++;
                log.warn("페이지 스킵 (pageNo={}): {}", pageNo, e.getMessage());
                continue;
            }

            List<PersonnelJobResponse.Item> items = response == null || response.getBody() == null
                    ? List.of() : response.getBody().itemList();
            if (items.isEmpty()) {
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

        log.info("공공취업정보 수집 완료: created={}, updated={}, skippedPages={}", created, updated, skippedPages);
    }

    private SyncResult upsertItems(List<PersonnelJobResponse.Item> items) {
        List<PersonnelJobResponse.Item> collectible = items.stream()
                .filter(this::isCollectible)
                .toList();
        if (collectible.isEmpty()) {
            return new SyncResult(0, 0);
        }

        List<String> externalIds = collectible.stream()
                .map(item -> String.valueOf(item.getIdx()))
                .distinct()
                .toList();

        Map<String, JobFeed> existing = jobFeedRepository
                .findByPlatformAndExternalIdIn(PLATFORM, externalIds).stream()
                .collect(Collectors.toMap(JobFeed::getExternalId, Function.identity(), (a, b) -> a));

        int created = 0;
        int updated = 0;
        for (PersonnelJobResponse.Item item : collectible) {
            String externalId = String.valueOf(item.getIdx());
            JobFeed found = existing.get(externalId);
            if (found != null) {
                found.update(
                        truncate(item.getInsttname(), COMPANY_MAX),
                        truncate(item.getTitle(), TITLE_MAX),
                        parseDeadline(item.getEnddate()),
                        null,
                        originalUrl(externalId),
                        parseCareerTypes(item.getType01(), item.getTitle()),
                        category(item.getType01(), item.getEnddate()),
                        region(item.getAreacode()),
                        truncate(item.getAreacode(), REGION_RAW_MAX));
                updated++;
            } else {
                jobFeedRepository.save(JobFeed.create(
                        externalId,
                        truncate(item.getInsttname(), COMPANY_MAX),
                        truncate(item.getTitle(), TITLE_MAX),
                        parseDeadline(item.getEnddate()),
                        null,
                        originalUrl(externalId),
                        PLATFORM,
                        parseCareerTypes(item.getType01(), item.getTitle()),
                        category(item.getType01(), item.getEnddate()),
                        region(item.getAreacode()),
                        truncate(item.getAreacode(), REGION_RAW_MAX)));
                created++;
            }
        }
        return new SyncResult(created, updated);
    }

    /**
     * 필수 필드(공고번호·기관명·제목)가 있고, 마감일이 오늘 이후(진행중)인 공고만 수집 대상.
     * 이 API는 진행중 여부(ongoingYn) 필드가 없어 마감일로 직접 판단한다.
     * 마감일을 파싱할 수 없는 공고는 진행중 여부를 알 수 없어 제외한다.
     */
    private boolean isCollectible(PersonnelJobResponse.Item item) {
        if (item.getIdx() == null
                || !StringUtils.hasText(item.getInsttname())
                || !StringUtils.hasText(item.getTitle())) {
            return false;
        }
        if (isResultAnnouncement(item.getTitle())) {
            return false;
        }
        LocalDate deadline = parseDeadline(item.getEnddate());
        return deadline != null && !deadline.isBefore(LocalDate.now());
    }

    /**
     * "최종합격자 공고" 등 채용이 아니라 결과 발표문인 공고는 채용정보로 부적절하므로 제외한다.
     */
    private boolean isResultAnnouncement(String title) {
        return RESULT_ANNOUNCEMENT_TITLE_KEYWORDS.stream().anyMatch(title::contains);
    }

    /**
     * type01("e02" 경력경쟁채용)은 확정적으로 경력, 그 외에는 제목에 "신입"/"경력"이 명시된
     * 경우만 추가로 인정한다. type01만으로는 신뢰도가 낮다 — 예를 들어 제목에 "경력경쟁채용시험"
     * 이라고 명시된 공고인데도 type01이 e01(공개경쟁채용)로 내려오는 사례가 실제로 있었다.
     * e01 등을 통째로 NEW로 넓히면 그런 오분류가 신입 필터에도 노출되므로, 제목에 명시된 경우만 인정한다.
     */
    private Set<CareerType> parseCareerTypes(String type01, String title) {
        Set<CareerType> result = EnumSet.noneOf(CareerType.class);
        if (CAREER_TYPE_EXPERIENCED_CODE.equalsIgnoreCase(type01)) {
            result.add(CareerType.EXPERIENCED);
        }
        if (StringUtils.hasText(title)) {
            if (title.contains("신입")) {
                result.add(CareerType.NEW);
            }
            if (title.contains("경력")) {
                result.add(CareerType.EXPERIENCED);
            }
        }
        return result;
    }

    private String category(String type01, String enddate) {
        if (NO_DEADLINE_SENTINEL.equals(enddate)) {
            return ONGOING_RECRUITMENT_LABEL;
        }
        return ANNOUNCEMENT_TYPE_LABELS.get(type01);
    }

    private String originalUrl(String externalId) {
        return String.format(ORIGINAL_URL_TEMPLATE, externalId);
    }

    private String region(String areacode) {
        return truncate(Region.fromAreaCode(areacode).getLabel(), REGION_MAX);
    }

    private LocalDate parseDeadline(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), DEADLINE_FORMAT);
        } catch (Exception e) {
            log.debug("마감일 파싱 실패, null 처리: {}", raw);
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

    private record SyncResult(int created, int updated) {
    }
}
