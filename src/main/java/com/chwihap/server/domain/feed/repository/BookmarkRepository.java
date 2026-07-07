package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.Bookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndJobPosting_Id(Long userId, Long jobPostingId);

    @Query("""
            SELECT jp.sourcePlatform, jp.sourceExternalId FROM Bookmark b
            JOIN b.jobPosting jp
            WHERE b.user.id = :userId AND b.isActive = true AND jp.sourcePlatform IS NOT NULL
            """)
    List<Object[]> findActiveSourceKeysByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT b FROM Bookmark b
            JOIN FETCH b.jobPosting jp
            WHERE b.user.id = :userId AND b.isActive = true
              AND (
                    :hasCursor = false
                    OR b.updatedAt < :cursorTime
                    OR (b.updatedAt = :cursorTime AND b.id < :cursorId)
                  )
            ORDER BY b.updatedAt DESC, b.id DESC
            """)
    List<Bookmark> findActivePage(@Param("userId") Long userId,
                                   @Param("hasCursor") boolean hasCursor,
                                   @Param("cursorTime") LocalDateTime cursorTime,
                                   @Param("cursorId") Long cursorId,
                                   Pageable pageable);
}
