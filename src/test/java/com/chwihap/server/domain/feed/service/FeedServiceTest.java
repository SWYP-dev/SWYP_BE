package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.feed.dto.FeedDetailResponse;
import com.chwihap.server.domain.feed.entity.Bookmark;
import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.BookmarkRepository;
import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private JobFeedRepository jobFeedRepository;
    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private KanbanCardRepository kanbanCardRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;

    private static final String DEFAULT_THUMBNAIL_URL = "https://chwihap.com/images/default-logo.png";

    private FeedService feedService;

    @BeforeEach
    void setUp() {
        feedService = new FeedService(
                jobFeedRepository,
                jobPostingRepository,
                bookmarkRepository,
                kanbanCardRepository,
                documentRepository,
                userRepository
        );
        ReflectionTestUtils.setField(feedService, "defaultThumbnailUrl", DEFAULT_THUMBNAIL_URL);
    }

    @Test
    void 카드가_남아있으면_스크랩_해제해도_JobPosting을_유지한다() {
        // Given
        Long userId = 1L;
        Long jobPostingId = 2L;
        stubBookmarkFound(userId, jobPostingId);
        when(kanbanCardRepository.existsByJobPosting_Id(jobPostingId)).thenReturn(true);

        // When
        feedService.removeScrap(userId, jobPostingId);

        // Then
        verify(jobPostingRepository, never()).deleteById(jobPostingId);
        verify(documentRepository, never()).findByUser_IdAndJobPosting_Id(userId, jobPostingId);
    }

    @Test
    void 카드가_없고_파일_문서가_없으면_스크랩_해제_시_Bookmark와_JobPosting을_하드_삭제한다() {
        // Given
        Long userId = 1L;
        Long jobPostingId = 2L;
        Document link = mock(Document.class);
        Document memo = mock(Document.class);
        Bookmark bookmark = stubBookmarkFound(userId, jobPostingId);
        when(kanbanCardRepository.existsByJobPosting_Id(jobPostingId)).thenReturn(false);
        when(documentRepository.findByUser_IdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(List.of(link, memo));
        when(link.getDocType()).thenReturn(DocumentType.LINK);
        when(memo.getDocType()).thenReturn(DocumentType.MEMO);

        // When
        feedService.removeScrap(userId, jobPostingId);

        // Then
        verify(documentRepository).deleteAll(List.of(link, memo));
        verify(link, never()).softDelete();
        verify(memo, never()).softDelete();
        // FK 위반 방지를 위해 JobPosting을 지우기 전 이 Bookmark row 자체도 함께 하드 삭제되어야 한다.
        verify(bookmarkRepository).delete(bookmark);
        verify(jobPostingRepository).deleteById(jobPostingId);
    }

    @Test
    void 카드가_없어도_FILE_문서가_있으면_스크랩_해제_시_파일은_soft_delete하고_JobPosting은_유지한다() {
        // Given
        Long userId = 1L;
        Long jobPostingId = 2L;
        Document file = mock(Document.class);
        stubBookmarkFound(userId, jobPostingId);
        when(kanbanCardRepository.existsByJobPosting_Id(jobPostingId)).thenReturn(false);
        when(documentRepository.findByUser_IdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(List.of(file));
        when(file.getDocType()).thenReturn(DocumentType.FILE);

        // When
        feedService.removeScrap(userId, jobPostingId);

        // Then
        verify(file).softDelete();
        verify(documentRepository, never()).deleteAll(anyList());
        verify(jobPostingRepository, never()).deleteById(jobPostingId);
    }

    @Test
    void 썸네일이_있으면_그대로_반환한다() {
        // Given
        Long userId = 1L;
        Long feedId = 2L;
        JobFeed feed = stubFeed(feedId, "https://cdn.example.com/thumb.png");
        when(jobPostingRepository.findByUserIdAndSourcePlatformAndSourceExternalId(
                userId, feed.getPlatform(), feed.getExternalId()))
                .thenReturn(Optional.empty());

        // When
        FeedDetailResponse response = feedService.getFeedDetail(userId, feedId);

        // Then
        assertThat(response.thumbnailUrl()).isEqualTo("https://cdn.example.com/thumb.png");
    }

    @Test
    void 썸네일이_없으면_기본_로고_URL로_대체한다() {
        // Given
        Long userId = 1L;
        Long feedId = 2L;
        JobFeed feed = stubFeed(feedId, null);
        when(jobPostingRepository.findByUserIdAndSourcePlatformAndSourceExternalId(
                userId, feed.getPlatform(), feed.getExternalId()))
                .thenReturn(Optional.empty());

        // When
        FeedDetailResponse response = feedService.getFeedDetail(userId, feedId);

        // Then
        assertThat(response.thumbnailUrl()).isEqualTo(DEFAULT_THUMBNAIL_URL);
    }

    @Test
    void 스크랩_목록에서도_썸네일이_없으면_기본_로고_URL로_대체한다() {
        // Given
        Long userId = 1L;
        Long jobPostingId = 2L;
        JobPosting posting = mock(JobPosting.class);
        when(posting.getId()).thenReturn(jobPostingId);
        when(posting.getThumbnailUrl()).thenReturn("");
        Bookmark bookmark = mock(Bookmark.class);
        when(bookmark.getJobPosting()).thenReturn(posting);
        when(bookmarkRepository.findActivePage(eq(userId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(bookmark)));
        when(kanbanCardRepository.existsByJobPosting_Id(jobPostingId)).thenReturn(false);

        // When
        var response = feedService.getScraps(userId, null, null);

        // Then
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).thumbnailUrl()).isEqualTo(DEFAULT_THUMBNAIL_URL);
    }

    // Given
    private JobFeed stubFeed(Long feedId, String thumbnailUrl) {
        JobFeed feed = mock(JobFeed.class);
        when(jobFeedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(feed.getPlatform()).thenReturn(JobPlatform.SARAMIN);
        when(feed.getExternalId()).thenReturn("ext-1");
        when(feed.getThumbnailUrl()).thenReturn(thumbnailUrl);
        when(feed.getDeadline()).thenReturn(LocalDate.of(2026, 8, 1));
        return feed;
    }

    // Given
    private Bookmark stubBookmarkFound(Long userId, Long jobPostingId) {
        Bookmark bookmark = mock(Bookmark.class);
        when(bookmarkRepository.findByUserIdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(Optional.of(bookmark));
        return bookmark;
    }
}
