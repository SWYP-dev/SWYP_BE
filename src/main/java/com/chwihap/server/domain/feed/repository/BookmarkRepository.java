package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.Bookmark;
import com.chwihap.server.domain.feed.enums.CareerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndJobPosting_Id(Long userId, Long jobPostingId);

    boolean existsByJobPosting_Id(Long jobPostingId);

    @Query("""
            SELECT jp.sourcePlatform, jp.sourceExternalId, jp.id FROM Bookmark b
            JOIN b.jobPosting jp
            WHERE b.user.id = :userId AND b.isActive = true AND jp.sourcePlatform IS NOT NULL
            """)
    List<Object[]> findActiveSourceKeysByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT b FROM Bookmark b
            JOIN FETCH b.jobPosting jp
            WHERE b.user.id = :userId AND b.isActive = true
              AND (:hasCategoryFilter = false OR jp.category IN :categories)
              AND (:hasCareerFilter = false OR jp.careerType IN :careers)
              AND (:hasRegionFilter = false OR jp.region IN :regions)
              AND (:deadlineSoon = false OR (jp.deadline IS NOT NULL AND jp.deadline BETWEEN :today AND :soonUntil))
            ORDER BY b.updatedAt DESC, b.id DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Bookmark b
            JOIN b.jobPosting jp
            WHERE b.user.id = :userId AND b.isActive = true
              AND (:hasCategoryFilter = false OR jp.category IN :categories)
              AND (:hasCareerFilter = false OR jp.careerType IN :careers)
              AND (:hasRegionFilter = false OR jp.region IN :regions)
              AND (:deadlineSoon = false OR (jp.deadline IS NOT NULL AND jp.deadline BETWEEN :today AND :soonUntil))
            """)
    Page<Bookmark> findActivePage(@Param("userId") Long userId,
                                   @Param("hasCategoryFilter") boolean hasCategoryFilter,
                                   @Param("categories") List<String> categories,
                                   @Param("hasCareerFilter") boolean hasCareerFilter,
                                   @Param("careers") List<CareerType> careers,
                                   @Param("hasRegionFilter") boolean hasRegionFilter,
                                   @Param("regions") List<String> regions,
                                   @Param("deadlineSoon") boolean deadlineSoon,
                                   @Param("today") LocalDate today,
                                   @Param("soonUntil") LocalDate soonUntil,
                                   Pageable pageable);
}
