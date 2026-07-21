package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.feed.dto.*;
import com.chwihap.server.domain.feed.entity.Bookmark;
import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.FeedSort;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.BookmarkRepository;
import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int DEADLINE_SOON_DAYS = 7;

    private final JobFeedRepository jobFeedRepository;
    private final JobPostingRepository jobPostingRepository;
    private final BookmarkRepository bookmarkRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Value("${app.feed.default-thumbnail-url}")
    private String defaultThumbnailUrl;

    /**
     * 2.1 공고 피드 조회 (페이지 번호 기반 페이지네이션).
     *
     * @param userId       조회 사용자 ID
     * @param page         페이지 번호 (0부터 시작, 기본 0)
     * @param size         페이지 크기 (기본 20, 최대 50)
     * @param sort         정렬 기준 (기본 LATEST)
     * @param platform     플랫폼 필터 (콤마 구분 다중)
     * @param jobCategory  직무 카테고리 필터 (콤마 구분 다중)
     * @param career       경력 구분 필터 (콤마 구분 다중)
     * @param region       지역 필터 (콤마 구분 다중)
     * @param deadlineSoon 마감 임박(7일 이내) 여부
     * @param keyword      기업명·직무명 키워드
     * @return 페이지 메타데이터를 포함한 공고 목록
     */
    public FeedListResponse getFeed(Long userId, Integer page, Integer size, FeedSort sort,
                                     String platform, String jobCategory, String career, String region,
                                     boolean deadlineSoon, String keyword) {
        int pageNumber = resolvePage(page);
        int pageSize = resolveSize(size);
        FeedSort resolvedSort = sort == null ? FeedSort.LATEST : sort;
        List<JobPlatform> platforms = parsePlatforms(platform);
        boolean hasCategoryFilter = jobCategory != null && !jobCategory.isBlank();
        boolean hasCareerFilter = career != null && !career.isBlank();
        boolean hasRegionFilter = region != null && !region.isBlank();
        List<String> categories = splitCsv(jobCategory);
        List<CareerType> careers = parseCareers(career);
        List<String> regions = splitCsv(region);
        LocalDate today = LocalDate.now();
        LocalDate soonUntil = today.plusDays(DEADLINE_SOON_DAYS);
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);

        Page<JobFeed> result;
        if (resolvedSort == FeedSort.DEADLINE) {
            result = jobFeedRepository.findDeadlinePage(platforms,
                    hasCategoryFilter, categories, hasCareerFilter, careers, hasRegionFilter, regions,
                    deadlineSoon, today, soonUntil, keyword, pageRequest);
        } else {
            result = jobFeedRepository.findLatestPage(platforms,
                    hasCategoryFilter, categories, hasCareerFilter, careers, hasRegionFilter, regions,
                    deadlineSoon, today, soonUntil, keyword, pageRequest);
        }

        Set<String> scrapKeys = activeScrapSourceKeys(userId);
        List<FeedItemResponse> items = result.getContent().stream()
                .map(feed -> toFeedItemResponse(feed, today, scrapKeys))
                .collect(Collectors.toList());

        return new FeedListResponse(items, result.getNumber(), result.getSize(),
                result.getTotalPages(), result.getTotalElements(), result.hasNext());
    }

    /**
     * 2.2 공고 상세 조회.
     *
     * @param userId 조회 사용자 ID
     * @param feedId 피드(job_feed) ID
     * @return 공고 상세 정보
     * @throws BusinessException 공고를 찾을 수 없는 경우
     */
    public FeedDetailResponse getFeedDetail(Long userId, Long feedId) {
        JobFeed feed = jobFeedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POSTING_NOT_FOUND));

        JobPosting posting = jobPostingRepository
                .findByUserIdAndSourcePlatformAndSourceExternalId(userId, feed.getPlatform(), feed.getExternalId())
                .orElse(null);

        boolean isScrapped = posting != null && bookmarkRepository
                .findByUserIdAndJobPosting_Id(userId, posting.getId())
                .map(Bookmark::isActive)
                .orElse(false);

        boolean isKanbanRegistered = posting != null
                && kanbanCardRepository.existsByJobPosting_Id(posting.getId());

        return new FeedDetailResponse(
                feed.getId(),
                feed.getPlatform().name(),
                feed.getCompanyName(),
                feed.getTitle(),
                feed.getCategory(),
                careerToString(feed.getCareerTypes()),
                feed.getRegion(),
                feed.getDeadline(),
                null, // TODO: job_feed에 본문 컬럼이 없어 우선 null 반환 (docs/취합_API_명세서 2.2 참고)
                resolveThumbnailUrl(feed.getThumbnailUrl()),
                feed.getOriginalUrl(),
                isScrapped,
                isKanbanRegistered,
                feed.getCrawledAt()
        );
    }

    /**
     * 2.3 스크랩 추가. 피드 공고를 유저 사본(job_postings)으로 복사한 뒤 스크랩을 활성화한다.
     *
     * @param userId 사용자 ID
     * @param feedId 피드(job_feed) ID
     * @return 스크랩 추가 결과 (피드 ID + 사본 jobPostingId)
     * @throws BusinessException 공고를 찾을 수 없는 경우
     */
    @Transactional
    public ScrapAddResponse addScrap(Long userId, Long feedId) {
        JobFeed feed = jobFeedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POSTING_NOT_FOUND));

        JobPosting posting = jobPostingRepository
                .findByUserIdAndSourcePlatformAndSourceExternalId(userId, feed.getPlatform(), feed.getExternalId())
                .orElseGet(() -> {
                    User userRef = userRepository.getReferenceById(userId);
                    return jobPostingRepository.save(JobPosting.copyFromFeed(feed, userRef));
                });

        Bookmark bookmark = bookmarkRepository.findByUserIdAndJobPosting_Id(userId, posting.getId())
                .orElseGet(() -> {
                    User userRef = userRepository.getReferenceById(userId);
                    return Bookmark.create(userRef, posting);
                });
        bookmark.activate();
        bookmarkRepository.save(bookmark);

        return new ScrapAddResponse(feed.getId(), posting.getId(), true);
    }

    /**
     * 2.4 스크랩 해제.
     *
     * @param userId        사용자 ID
     * @param jobPostingId  유저 사본(job_postings) ID
     * @return 스크랩 해제 결과
     * @throws BusinessException 스크랩 내역을 찾을 수 없는 경우
     */
    @Transactional
    public ScrapRemoveResponse removeScrap(Long userId, Long jobPostingId) {
        Bookmark bookmark = bookmarkRepository.findByUserIdAndJobPosting_Id(userId, jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_NOT_FOUND));

        bookmark.deactivate();
        bookmarkRepository.save(bookmark);

        // Bookmark와 KanbanCard는 JobPosting에 대해 독립된 참조이므로, 이 공고를 참조하는
        // KanbanCard가 남아있지 않을 때만 JobPosting을 함께 정리한다.
        boolean cardExists = kanbanCardRepository.existsByJobPosting_Id(jobPostingId);
        if (!cardExists) {
            List<Document> documents = documentRepository.findByUser_IdAndJobPosting_Id(userId, jobPostingId);
            List<Document> fileDocuments = documents.stream()
                    .filter(document -> document.getDocType() == DocumentType.FILE)
                    .toList();
            List<Document> nonFileDocuments = documents.stream()
                    .filter(document -> document.getDocType() != DocumentType.FILE)
                    .toList();

            // FILE은 S3 정리가 필요해 soft delete 후 배치가 처리, LINK/MEMO는 S3 의존이 없어 즉시 hard delete.
            fileDocuments.forEach(Document::softDelete);
            if (!nonFileDocuments.isEmpty()) {
                documentRepository.deleteAll(nonFileDocuments);
                documentRepository.flush();
            }

            if (fileDocuments.isEmpty()) {
                // FK 위반 방지: JobPosting을 지우기 전에 방금 비활성화한 이 Bookmark row 자체도 함께 정리한다.
                bookmarkRepository.delete(bookmark);
                bookmarkRepository.flush();
                jobPostingRepository.deleteById(jobPostingId);
                jobPostingRepository.flush();
            }
        }

        return new ScrapRemoveResponse(jobPostingId, false);
    }

    /**
     * 2.5 스크랩 목록 조회 (페이지 번호 기반 페이지네이션).
     *
     * @param userId 사용자 ID
     * @param page   페이지 번호 (0부터 시작, 기본 0)
     * @param size   페이지 크기 (기본 20, 최대 50)
     * @return 페이지 메타데이터를 포함한 스크랩 목록
     */
    public ScrapListResponse getScraps(Long userId, Integer page, Integer size) {
        int pageNumber = resolvePage(page);
        int pageSize = resolveSize(size);
        Page<Bookmark> result = bookmarkRepository.findActivePage(userId, PageRequest.of(pageNumber, pageSize));

        List<ScrapListItemResponse> items = result.getContent().stream()
                .map(bookmark -> {
                    JobPosting posting = bookmark.getJobPosting();
                    boolean isKanbanRegistered = kanbanCardRepository.existsByJobPosting_Id(posting.getId());
                    return new ScrapListItemResponse(
                            posting.getId(),
                            posting.getCompanyName(),
                            posting.getTitle(),
                            posting.getDeadline(),
                            resolveThumbnailUrl(posting.getThumbnailUrl()),
                            posting.getOriginalUrl(),
                            isKanbanRegistered,
                            bookmark.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());

        return new ScrapListResponse(items, result.getNumber(), result.getSize(),
                result.getTotalPages(), result.getTotalElements(), result.hasNext());
    }

    private Set<String> activeScrapSourceKeys(Long userId) {
        Set<String> keys = new HashSet<>();
        for (Object[] row : bookmarkRepository.findActiveSourceKeysByUserId(userId)) {
            JobPlatform sourcePlatform = (JobPlatform) row[0];
            String sourceExternalId = (String) row[1];
            keys.add(sourcePlatform.name() + ":" + sourceExternalId);
        }
        return keys;
    }

    private FeedItemResponse toFeedItemResponse(JobFeed feed, LocalDate today, Set<String> scrapKeys) {
        boolean isScrapped = scrapKeys.contains(feed.getPlatform().name() + ":" + feed.getExternalId());
        boolean isExpired = feed.getDeadline() != null && feed.getDeadline().isBefore(today);
        return new FeedItemResponse(
                feed.getId(),
                feed.getPlatform().name(),
                feed.getCompanyName(),
                feed.getTitle(),
                feed.getCategory(),
                careerToString(feed.getCareerTypes()),
                feed.getRegion(),
                feed.getDeadline(),
                resolveThumbnailUrl(feed.getThumbnailUrl()),
                feed.getOriginalUrl(),
                isScrapped,
                isExpired,
                feed.getCrawledAt()
        );
    }

    /**
     * job_feed의 다중 채용구분(Set)을 응답용 콤마 문자열로 변환한다. (예: "NEW,EXPERIENCED")
     * 값이 없으면 null을 반환한다. 순서는 enum 정의 순으로 안정화한다.
     */
    private String careerToString(Set<CareerType> careerTypes) {
        if (careerTypes == null || careerTypes.isEmpty()) {
            return null;
        }
        return careerTypes.stream()
                .sorted()
                .map(CareerType::name)
                .collect(Collectors.joining(","));
    }

    /**
     * 공고에 썸네일이 없으면(null/빈 값) 기업 로고 영역에 노출할 서비스 기본 로고 URL로 대체한다.
     */
    private String resolveThumbnailUrl(String thumbnailUrl) {
        return StringUtils.hasText(thumbnailUrl) ? thumbnailUrl : defaultThumbnailUrl;
    }

    private int resolvePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int resolveSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private List<JobPlatform> parsePlatforms(String platform) {
        if (platform == null || platform.isBlank()) {
            return Arrays.asList(JobPlatform.values());
        }
        try {
            return Arrays.stream(platform.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(JobPlatform::valueOf)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private List<CareerType> parseCareers(String career) {
        if (career == null || career.isBlank()) {
            return List.of();
        }
        try {
            List<CareerType> parsed = Arrays.stream(career.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(CareerType::valueOf)
                    .collect(Collectors.toList());
            return parsed.isEmpty() ? List.of(CareerType.NEW) : parsed;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 콤마 구분 필터값을 파싱한다. 필터가 없을 때는 쿼리의 IN 절 바인딩이 깨지지 않도록
     * 더미 값 하나를 채운 리스트를 반환한다 (실제로는 hasXxxFilter=false라 조건 자체가 무시됨).
     */
    private List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("");
        }
        List<String> parsed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return parsed.isEmpty() ? List.of("") : parsed;
    }
}
