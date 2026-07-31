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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 잡아바 채용정보 API 응답을 {@link JobFeed}로 변환해 (platform=JOBABA, externalId) 기준으로 upsert한다.
 * 페이지 순회는 트랜잭션 밖에서 수행하고, 페이지 단위로만 트랜잭션을 연다.
 * <p>
 * 이 API는 공고 고유번호를 제공하지 않아, 공고별로 고유한 원문 {@code URL}을 externalId로 사용한다.
 * 또한 {@code list_total_count}가 14만 건을 넘는 전체 누적 이력이고 날짜 필터·정렬 보장이 없어,
 * 매 실행마다 전체 페이지를 순회하되 마감(과거 접수종료일) 공고는 애초에 저장하지 않는 방식으로
 * 대응한다. 여러 번 스케줄이 돌면서 커버리지가 자연히 채워지는 구조라 별도의 조기 종료
 * 휴리스틱은 두지 않는다.
 */
@Slf4j
@Service
public class JobFeedJobabaSyncService {

    private static final JobPlatform PLATFORM = JobPlatform.JOBABA;
    private static final int COMPANY_MAX = 255;
    private static final int TITLE_MAX = 255;
    private static final int URL_MAX = 1000;
    private static final int REGION_MAX = 100;
    private static final int REGION_RAW_MAX = 500;
    private static final int CATEGORY_MAX = 255;
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobFeedRepository jobFeedRepository;
    private final JobabaApiClient jobabaApiClient;
    private final JobabaProperties properties;
    private final JobCategoryClassifier categoryClassifier;
    private final TransactionTemplate transactionTemplate;

    public JobFeedJobabaSyncService(JobFeedRepository jobFeedRepository,
                                     JobabaApiClient jobabaApiClient,
                                     JobabaProperties properties,
                                     JobCategoryClassifier categoryClassifier,
                                     PlatformTransactionManager transactionManager) {
        this.jobFeedRepository = jobFeedRepository;
        this.jobabaApiClient = jobabaApiClient;
        this.properties = properties;
        this.categoryClassifier = categoryClassifier;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 공고를 앞쪽 N페이지까지 순회하며 job_feed에 upsert한다.
     * 한 페이지 호출이 재시도 후에도 실패하면 그 페이지만 건너뛰고 계속 진행한다.
     */
    public void sync() {
        if (!StringUtils.hasText(properties.serviceKey())) {
            log.warn("잡아바 serviceKey가 비어 있어 공고 수집을 건너뜁니다. (환경변수 GG_DATA_DREAM_SERVICE_KEY 확인)");
            return;
        }

        int numOfRows = properties.sync().numOfRows();
        int maxPages = properties.sync().maxPages();
        int created = 0;
        int updated = 0;
        int skippedPages = 0;

        for (int pIndex = 1; pIndex <= maxPages; pIndex++) {
            JobabaResponse response;
            try {
                response = jobabaApiClient.fetchPage(pIndex, numOfRows);
            } catch (JobabaApiException e) {
                skippedPages++;
                log.warn("페이지 스킵 (pIndex={}): {}", pIndex, e.getMessage());
                continue;
            }

            List<JobabaResponse.Item> items = response == null ? List.of() : response.itemList();
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

        log.info("잡아바 공고 수집 완료: created={}, updated={}, skippedPages={}", created, updated, skippedPages);
    }

    private SyncResult upsertItems(List<JobabaResponse.Item> items) {
        List<JobabaResponse.Item> collectible = items.stream()
                .filter(this::isCollectible)
                .toList();
        if (collectible.isEmpty()) {
            return new SyncResult(0, 0);
        }

        List<String> externalIds = collectible.stream()
                .map(JobabaResponse.Item::url)
                .distinct()
                .toList();

        // 같은 페이지 안에서도 동일 URL(externalId)이 중복으로 나오는 경우가 있어(여러 채용 채널을
        // 취합하는 API 특성상), DB 조회 결과뿐 아니라 이번 배치에서 새로 만든 항목도 함께 추적해야
        // 두 번째 중복 항목이 신규 insert로 오인되어 유니크 제약을 위반하지 않는다.
        Map<String, JobFeed> known = new HashMap<>(jobFeedRepository
                .findByPlatformAndExternalIdIn(PLATFORM, externalIds).stream()
                .collect(Collectors.toMap(JobFeed::getExternalId, Function.identity(), (a, b) -> a)));

        int created = 0;
        int updated = 0;
        for (JobabaResponse.Item item : collectible) {
            String externalId = item.url();
            String companyName = truncate(item.entrprsNm(), COMPANY_MAX);
            String title = truncate(item.pbancCont(), TITLE_MAX);
            LocalDate deadline = parseDeadline(item.rcptEndDe());
            String originalUrl = truncate(item.url(), URL_MAX);
            Set<CareerType> careerTypes = parseCareerTypes(item.careerDiv());
            String category = category(item.pbancCont(), item.recrutFieldNm());
            String region = region(item.workRegionCdCont());
            String regionRaw = truncate(item.workRegionCont(), REGION_RAW_MAX);

            JobFeed found = known.get(externalId);
            if (found != null) {
                found.update(companyName, title, deadline, null, originalUrl,
                        careerTypes, category, region, regionRaw);
                updated++;
            } else {
                JobFeed saved = jobFeedRepository.save(JobFeed.create(externalId, companyName, title, deadline,
                        null, originalUrl, PLATFORM, careerTypes, category, region, regionRaw));
                known.put(externalId, saved);
                created++;
            }
        }
        return new SyncResult(created, updated);
    }

    /**
     * 기업명·공고명·URL(=externalId)이 있고, 접수종료일이 오늘 이후(진행중)인 공고만 수집 대상.
     * 이 API는 원본 목록 자체가 이미 마감된 과거 이력까지 포함하고 있어, 마감일로 직접 걸러낸다.
     */
    private boolean isCollectible(JobabaResponse.Item item) {
        if (!StringUtils.hasText(item.entrprsNm())
                || !StringUtils.hasText(item.pbancCont())
                || !StringUtils.hasText(item.url())) {
            return false;
        }
        LocalDate deadline = parseDeadline(item.rcptEndDe());
        return deadline != null && !deadline.isBefore(LocalDate.now());
    }

    /**
     * careerDiv 텍스트에 "신입"/"경력"이 포함되면 해당 값을 추가한다("신입/경력" 등 혼재 표기는 양쪽 다 추가).
     * "무관"처럼 둘 다 아닌 경우는 신입·경력 필터 양쪽에 노출되도록 양쪽 다 추가한다.
     */
    private Set<CareerType> parseCareerTypes(String careerDiv) {
        Set<CareerType> result = EnumSet.noneOf(CareerType.class);
        if (!StringUtils.hasText(careerDiv)) {
            return result;
        }
        if (careerDiv.contains("신입")) {
            result.add(CareerType.NEW);
        }
        if (careerDiv.contains("경력")) {
            result.add(CareerType.EXPERIENCED);
        }
        if (result.isEmpty()) {
            result.add(CareerType.NEW);
            result.add(CareerType.EXPERIENCED);
        }
        return result;
    }

    /**
     * 제목 키워드로 신규 직군을 확신 있게 추정할 수 있으면 그 값을 쓰고,
     * 아니면 원본 API가 내려주는 모집분야명(RECRUT_FIELD_NM)을 그대로 fallback으로 쓴다.
     */
    String category(String title, String recrutFieldNm) {
        String jobCategory = categoryClassifier.classify(title);
        if (jobCategory != null) {
            return truncate(jobCategory, CATEGORY_MAX);
        }
        return truncate(recrutFieldNm, CATEGORY_MAX);
    }

    /**
     * 근무지역코드(4자리, 앞 2자리가 시/도 코드) 기준으로 정규화한다.
     * 코드가 비어 있는 경우, 이 API 자체가 경기도일자리재단 소관 데이터라는 전제로 경기로 처리한다.
     */
    private String region(String workRegionCdCont) {
        Region region = StringUtils.hasText(workRegionCdCont)
                ? Region.fromAreaCode(workRegionCdCont)
                : Region.GYEONGGI;
        return truncate(region.getLabel(), REGION_MAX);
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
