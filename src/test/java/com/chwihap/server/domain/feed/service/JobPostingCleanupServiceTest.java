package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.feed.entity.Bookmark;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.repository.BookmarkRepository;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.kanban.repository.ApplicationPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostingCleanupServiceTest {

    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private ApplicationPostingRepository applicationPostingRepository;

    private JobPostingCleanupService jobPostingCleanupService;

    @BeforeEach
    void setUp() {
        jobPostingCleanupService = new JobPostingCleanupService(
                jobPostingRepository,
                bookmarkRepository,
                applicationPostingRepository
        );
    }

    @Test
    void ApplicationPosting이_원본을_참조하면_유지한다() {
        Long userId = 1L;
        Long jobPostingId = 2L;
        JobPosting jobPosting = mock(JobPosting.class);
        when(jobPostingRepository.findByIdAndUser_Id(jobPostingId, userId))
                .thenReturn(Optional.of(jobPosting));
        when(applicationPostingRepository.existsByUser_IdAndSourceJobPosting_Id(userId, jobPostingId))
                .thenReturn(true);

        jobPostingCleanupService.deleteIfOrphan(userId, jobPostingId);

        verifyNoInteractions(bookmarkRepository);
        verify(jobPostingRepository, never()).delete(any());
    }

    @Test
    void 활성_Bookmark가_있으면_유지한다() {
        Long userId = 1L;
        Long jobPostingId = 2L;
        JobPosting jobPosting = mock(JobPosting.class);
        Bookmark bookmark = mock(Bookmark.class);
        when(jobPostingRepository.findByIdAndUser_Id(jobPostingId, userId))
                .thenReturn(Optional.of(jobPosting));
        when(bookmarkRepository.findByUserIdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(Optional.of(bookmark));
        when(bookmark.isActive()).thenReturn(true);

        jobPostingCleanupService.deleteIfOrphan(userId, jobPostingId);

        verify(bookmarkRepository, never()).delete(any());
        verify(jobPostingRepository, never()).delete(any());
    }

    @Test
    void 비활성_Bookmark와_고아_JobPosting을_순서대로_삭제한다() {
        Long userId = 1L;
        Long jobPostingId = 2L;
        JobPosting jobPosting = mock(JobPosting.class);
        Bookmark bookmark = mock(Bookmark.class);
        when(jobPostingRepository.findByIdAndUser_Id(jobPostingId, userId))
                .thenReturn(Optional.of(jobPosting));
        when(bookmarkRepository.findByUserIdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(Optional.of(bookmark));
        when(bookmark.isActive()).thenReturn(false);

        jobPostingCleanupService.deleteIfOrphan(userId, jobPostingId);

        InOrder inOrder = inOrder(bookmarkRepository, jobPostingRepository);
        inOrder.verify(bookmarkRepository).delete(bookmark);
        inOrder.verify(bookmarkRepository).flush();
        inOrder.verify(jobPostingRepository).delete(jobPosting);
        inOrder.verify(jobPostingRepository).flush();
    }

    @Test
    void Bookmark가_없는_고아_JobPosting을_삭제한다() {
        Long userId = 1L;
        Long jobPostingId = 2L;
        JobPosting jobPosting = mock(JobPosting.class);
        when(jobPostingRepository.findByIdAndUser_Id(jobPostingId, userId))
                .thenReturn(Optional.of(jobPosting));
        when(bookmarkRepository.findByUserIdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(Optional.empty());

        jobPostingCleanupService.deleteIfOrphan(userId, jobPostingId);

        verify(jobPostingRepository).delete(jobPosting);
        verify(jobPostingRepository).flush();
        verify(bookmarkRepository, never()).delete(any());
    }
}
