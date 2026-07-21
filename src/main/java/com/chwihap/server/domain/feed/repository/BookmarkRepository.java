package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndJobPosting_Id(Long userId, Long jobPostingId);

    boolean existsByJobPosting_Id(Long jobPostingId);

    @Query("""
            SELECT jp.sourcePlatform, jp.sourceExternalId FROM Bookmark b
            JOIN b.jobPosting jp
            WHERE b.user.id = :userId AND b.isActive = true AND jp.sourcePlatform IS NOT NULL
            """)
    List<Object[]> findActiveSourceKeysByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT b FROM Bookmark b
            JOIN FETCH b.jobPosting jp
            WHERE b.user.id = :userId AND b.isActive = true
            ORDER BY b.updatedAt DESC, b.id DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Bookmark b
            WHERE b.user.id = :userId AND b.isActive = true
            """)
    Page<Bookmark> findActivePage(@Param("userId") Long userId, Pageable pageable);
}
