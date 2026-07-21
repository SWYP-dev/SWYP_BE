package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.feed.entity.Bookmark;
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

import java.util.List;
import java.util.Optional;

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

    // Given
    private Bookmark stubBookmarkFound(Long userId, Long jobPostingId) {
        Bookmark bookmark = mock(Bookmark.class);
        when(bookmarkRepository.findByUserIdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(Optional.of(bookmark));
        return bookmark;
    }
}
