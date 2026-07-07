package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JobFeedRepository extends JpaRepository<JobFeed, Long> {

    @Query("""
            SELECT f FROM JobFeed f
            WHERE f.platform IN :platforms
              AND (:hasCategoryFilter = false OR f.category IN :categories)
              AND (:hasCareerFilter = false OR f.careerType IN :careers)
              AND (:hasRegionFilter = false OR f.region IN :regions)
              AND (:deadlineSoon = false OR (f.deadline IS NOT NULL AND f.deadline BETWEEN :today AND :soonUntil))
              AND (:keyword IS NULL
                    OR LOWER(f.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:cursorId IS NULL OR f.id < :cursorId)
            ORDER BY f.id DESC
            """)
    List<JobFeed> findLatestPage(@Param("platforms") List<JobPlatform> platforms,
                                  @Param("hasCategoryFilter") boolean hasCategoryFilter,
                                  @Param("categories") List<String> categories,
                                  @Param("hasCareerFilter") boolean hasCareerFilter,
                                  @Param("careers") List<CareerType> careers,
                                  @Param("hasRegionFilter") boolean hasRegionFilter,
                                  @Param("regions") List<String> regions,
                                  @Param("deadlineSoon") boolean deadlineSoon,
                                  @Param("today") LocalDate today,
                                  @Param("soonUntil") LocalDate soonUntil,
                                  @Param("keyword") String keyword,
                                  @Param("cursorId") Long cursorId,
                                  Pageable pageable);

    @Query("""
            SELECT f FROM JobFeed f
            WHERE f.platform IN :platforms
              AND (:hasCategoryFilter = false OR f.category IN :categories)
              AND (:hasCareerFilter = false OR f.careerType IN :careers)
              AND (:hasRegionFilter = false OR f.region IN :regions)
              AND (:deadlineSoon = false OR (f.deadline IS NOT NULL AND f.deadline BETWEEN :today AND :soonUntil))
              AND (:keyword IS NULL
                    OR LOWER(f.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (
                    :hasCursor = false
                    OR (:cursorDeadline IS NULL AND f.deadline IS NULL AND f.id > :cursorId)
                    OR (:cursorDeadline IS NOT NULL AND (
                            (f.deadline IS NOT NULL AND f.deadline > :cursorDeadline)
                            OR (f.deadline = :cursorDeadline AND f.id > :cursorId)
                            OR f.deadline IS NULL
                        ))
                  )
            ORDER BY CASE WHEN f.deadline IS NULL THEN 1 ELSE 0 END, f.deadline ASC, f.id ASC
            """)
    List<JobFeed> findDeadlinePage(@Param("platforms") List<JobPlatform> platforms,
                                    @Param("hasCategoryFilter") boolean hasCategoryFilter,
                                    @Param("categories") List<String> categories,
                                    @Param("hasCareerFilter") boolean hasCareerFilter,
                                    @Param("careers") List<CareerType> careers,
                                    @Param("hasRegionFilter") boolean hasRegionFilter,
                                    @Param("regions") List<String> regions,
                                    @Param("deadlineSoon") boolean deadlineSoon,
                                    @Param("today") LocalDate today,
                                    @Param("soonUntil") LocalDate soonUntil,
                                    @Param("keyword") String keyword,
                                    @Param("hasCursor") boolean hasCursor,
                                    @Param("cursorDeadline") LocalDate cursorDeadline,
                                    @Param("cursorId") Long cursorId,
                                    Pageable pageable);
}
