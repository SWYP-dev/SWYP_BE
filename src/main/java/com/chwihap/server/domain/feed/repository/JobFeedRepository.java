package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface JobFeedRepository extends JpaRepository<JobFeed, Long> {

    /**
     * 수집 배치의 upsert 대상 판별용. (platform, externalId) 조합으로 기존 공고들을 일괄 조회한다.
     */
    List<JobFeed> findByPlatformAndExternalIdIn(JobPlatform platform, Collection<String> externalIds);

    @Query("""
            SELECT f FROM JobFeed f
            WHERE f.platform IN :platforms
              AND (:hasCategoryFilter = false OR f.category IN :categories)
              AND (:hasCareerFilter = false
                    OR EXISTS (SELECT ct FROM JobFeed jf JOIN jf.careerTypes ct WHERE jf = f AND ct IN :careers))
              AND (:hasRegionFilter = false OR f.region IN :regions)
              AND (:deadlineSoon = false OR (f.deadline IS NOT NULL AND f.deadline BETWEEN :today AND :soonUntil))
              AND (:keyword IS NULL
                    OR LOWER(f.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY f.id DESC
            """)
    Page<JobFeed> findLatestPage(@Param("platforms") List<JobPlatform> platforms,
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
                                  Pageable pageable);

    @Query("""
            SELECT f FROM JobFeed f
            WHERE f.platform IN :platforms
              AND (:hasCategoryFilter = false OR f.category IN :categories)
              AND (:hasCareerFilter = false
                    OR EXISTS (SELECT ct FROM JobFeed jf JOIN jf.careerTypes ct WHERE jf = f AND ct IN :careers))
              AND (:hasRegionFilter = false OR f.region IN :regions)
              AND (:deadlineSoon = false OR (f.deadline IS NOT NULL AND f.deadline BETWEEN :today AND :soonUntil))
              AND (:keyword IS NULL
                    OR LOWER(f.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY CASE WHEN f.deadline IS NULL THEN 1 ELSE 0 END, f.deadline ASC, f.id ASC
            """)
    Page<JobFeed> findDeadlinePage(@Param("platforms") List<JobPlatform> platforms,
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
                                    Pageable pageable);
}
