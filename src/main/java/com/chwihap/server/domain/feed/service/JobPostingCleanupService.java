package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.feed.entity.Bookmark;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.repository.BookmarkRepository;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.kanban.repository.ApplicationPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobPostingCleanupService {

    private final JobPostingRepository jobPostingRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ApplicationPostingRepository applicationPostingRepository;

    /**
     * 활성 스크랩과 지원 공고 스냅샷 어디에서도 사용하지 않는 유저 공고 사본을 정리한다.
     */
    @Transactional
    public void deleteIfOrphan(Long userId, Long jobPostingId) {
        if (jobPostingId == null) {
            return;
        }

        JobPosting jobPosting = jobPostingRepository.findByIdAndUser_Id(jobPostingId, userId)
                .orElse(null);
        if (jobPosting == null || applicationPostingRepository
                .existsByUser_IdAndSourceJobPosting_Id(userId, jobPostingId)) {
            return;
        }

        Optional<Bookmark> bookmark = bookmarkRepository
                .findByUserIdAndJobPosting_Id(userId, jobPostingId);
        if (bookmark.map(Bookmark::isActive).orElse(false)) {
            return;
        }

        bookmark.ifPresent(bookmarkRepository::delete);
        if (bookmark.isPresent()) {
            bookmarkRepository.flush();
        }

        jobPostingRepository.delete(jobPosting);
        jobPostingRepository.flush();
    }
}
