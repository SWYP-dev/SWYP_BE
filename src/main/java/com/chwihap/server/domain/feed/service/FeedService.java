package com.chwihap.server.domain.feed.service;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final UserRepository userRepository;

    public FeedListResponse getFeed(Long userId, String cursor, Integer size, FeedSort sort,
                                     String platform, String jobCategory, String career, String region,
                                     boolean deadlineSoon, String keyword) {
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
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<JobFeed> rows;
        if (resolvedSort == FeedSort.DEADLINE) {
            boolean hasCursor = cursor != null;
            Long cursorId = null;
            LocalDate cursorDeadline = null;
            if (hasCursor) {
                FeedCursor decoded = CursorCodec.decode(cursor, FeedCursor.class);
                cursorId = decoded.id();
                cursorDeadline = decoded.deadline() == null ? null : LocalDate.parse(decoded.deadline());
            }
            rows = jobFeedRepository.findDeadlinePage(platforms,
                    hasCategoryFilter, categories, hasCareerFilter, careers, hasRegionFilter, regions,
                    deadlineSoon, today, soonUntil, keyword, hasCursor, cursorDeadline, cursorId, pageRequest);
        } else {
            Long cursorId = cursor == null ? null : CursorCodec.decode(cursor, FeedCursor.class).id();
            rows = jobFeedRepository.findLatestPage(platforms,
                    hasCategoryFilter, categories, hasCareerFilter, careers, hasRegionFilter, regions,
                    deadlineSoon, today, soonUntil, keyword, cursorId, pageRequest);
        }

        boolean hasNext = rows.size() > pageSize;
        List<JobFeed> pageRows = hasNext ? rows.subList(0, pageSize) : rows;

        Set<String> favoriteKeys = activeFavoriteSourceKeys(userId);

        List<FeedItemResponse> items = pageRows.stream()
                .map(feed -> toFeedItemResponse(feed, today, favoriteKeys))
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasNext && !pageRows.isEmpty()) {
            JobFeed last = pageRows.get(pageRows.size() - 1);
            String deadlineIso = last.getDeadline() == null ? null : last.getDeadline().toString();
            nextCursor = CursorCodec.encode(new FeedCursor(last.getId(), deadlineIso));
        }

        return new FeedListResponse(items, nextCursor, hasNext);
    }

    public FeedDetailResponse getFeedDetail(Long userId, Long feedId) {
        JobFeed feed = jobFeedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POSTING_NOT_FOUND));

        JobPosting posting = jobPostingRepository
                .findByUserIdAndSourcePlatformAndSourceExternalId(userId, feed.getPlatform(), feed.getExternalId())
                .orElse(null);

        boolean isFavorite = posting != null && bookmarkRepository
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
                feed.getCareerType() == null ? null : feed.getCareerType().name(),
                feed.getDeadline(),
                null, // TODO: job_feed에 본문 컬럼이 없어 우선 null 반환 (docs/취합_API_명세서 2.2 참고)
                feed.getThumbnailUrl(),
                feed.getOriginalUrl(),
                isFavorite,
                isKanbanRegistered,
                feed.getCrawledAt()
        );
    }

    @Transactional
    public FavoriteAddResponse addFavorite(Long userId, Long feedId) {
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

        return new FavoriteAddResponse(feed.getId(), posting.getId(), true);
    }

    @Transactional
    public FavoriteRemoveResponse removeFavorite(Long userId, Long jobPostingId) {
        Bookmark bookmark = bookmarkRepository.findByUserIdAndJobPosting_Id(userId, jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_NOT_FOUND));

        bookmark.deactivate();
        bookmarkRepository.save(bookmark);

        return new FavoriteRemoveResponse(jobPostingId, false);
    }

    public FavoriteListResponse getFavorites(Long userId, String cursor, Integer size) {
        int pageSize = resolveSize(size);
        boolean hasCursor = cursor != null;
        LocalDateTime cursorTime = null;
        Long cursorId = null;
        if (hasCursor) {
            FavoriteCursor decoded = CursorCodec.decode(cursor, FavoriteCursor.class);
            cursorTime = LocalDateTime.parse(decoded.updatedAt());
            cursorId = decoded.id();
        }

        List<Bookmark> rows = bookmarkRepository.findActivePage(
                userId, hasCursor, cursorTime, cursorId, PageRequest.of(0, pageSize + 1));

        boolean hasNext = rows.size() > pageSize;
        List<Bookmark> pageRows = hasNext ? rows.subList(0, pageSize) : rows;

        List<FavoriteListItemResponse> items = pageRows.stream()
                .map(bookmark -> {
                    JobPosting posting = bookmark.getJobPosting();
                    boolean isKanbanRegistered = kanbanCardRepository.existsByJobPosting_Id(posting.getId());
                    return new FavoriteListItemResponse(
                            posting.getId(),
                            posting.getCompanyName(),
                            posting.getTitle(),
                            posting.getDeadline(),
                            posting.getThumbnailUrl(),
                            posting.getOriginalUrl(),
                            isKanbanRegistered,
                            bookmark.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasNext && !pageRows.isEmpty()) {
            Bookmark last = pageRows.get(pageRows.size() - 1);
            nextCursor = CursorCodec.encode(new FavoriteCursor(last.getId(), last.getUpdatedAt().toString()));
        }

        return new FavoriteListResponse(items, nextCursor, hasNext);
    }

    private Set<String> activeFavoriteSourceKeys(Long userId) {
        Set<String> keys = new HashSet<>();
        for (Object[] row : bookmarkRepository.findActiveSourceKeysByUserId(userId)) {
            JobPlatform sourcePlatform = (JobPlatform) row[0];
            String sourceExternalId = (String) row[1];
            keys.add(sourcePlatform.name() + ":" + sourceExternalId);
        }
        return keys;
    }

    private FeedItemResponse toFeedItemResponse(JobFeed feed, LocalDate today, Set<String> favoriteKeys) {
        boolean isFavorite = favoriteKeys.contains(feed.getPlatform().name() + ":" + feed.getExternalId());
        boolean isExpired = feed.getDeadline() != null && feed.getDeadline().isBefore(today);
        return new FeedItemResponse(
                feed.getId(),
                feed.getPlatform().name(),
                feed.getCompanyName(),
                feed.getTitle(),
                feed.getCategory(),
                feed.getCareerType() == null ? null : feed.getCareerType().name(),
                feed.getDeadline(),
                feed.getThumbnailUrl(),
                feed.getOriginalUrl(),
                isFavorite,
                isExpired,
                feed.getCrawledAt()
        );
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
