package com.chwihap.server.domain.feed.entity;

import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(
        name = "job_feeds",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_feed_platform_external_id",
                        columnNames = {"platform", "external_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_job_fed_deadline",
                        columnList = "deadline"
                ),
                @Index(
                        name = "idx_job_feed_region",
                        columnList = "region"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true)
    private LocalDate deadline;

    @Column(nullable = true, length = 500)
    private String thumbnailUrl;

    @Column(nullable = true, length = 1000)
    private String originalUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPlatform platform;

    /**
     * 채용구분(신입/경력). "신입+경력" 공고는 두 값을 모두 담아 신입·경력 필터 양쪽에 노출된다.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "job_feed_career_types",
            joinColumns = @JoinColumn(name = "job_feed_id"),
            foreignKey = @ForeignKey(name = "fk_job_feed_career_types_job_feed")
    )
    @Column(name = "career_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 100)
    private Set<CareerType> careerTypes = EnumSet.noneOf(CareerType.class);

    @Column(nullable = true, length = 255)
    private String category;

    /**
     * 필터(정확 매칭)용 대표 지역. 다지역 공고는 첫 번째 근무지역만 저장한다.
     */
    @Column(nullable = true, length = 100)
    private String region;

    /**
     * 표시용 원문 근무지역(콤마 구분 다지역 전체). 필터에는 사용하지 않는다.
     */
    @Column(nullable = true, length = 500)
    private String regionRaw;

    @Column(nullable = false)
    private LocalDateTime crawledAt;

    public static JobFeed create(String externalId, String companyName, String title, LocalDate deadline,
                                  String thumbnailUrl, String originalUrl, JobPlatform platform,
                                  Set<CareerType> careerTypes, String category, String region, String regionRaw) {
        JobFeed feed = new JobFeed();
        feed.externalId = externalId;
        feed.platform = platform;
        feed.applyMutableFields(companyName, title, deadline, thumbnailUrl, originalUrl,
                careerTypes, category, region, regionRaw);
        return feed;
    }

    /**
     * 재수집 시 (platform, externalId)로 매칭된 기존 공고의 변경 가능한 필드를 갱신한다.
     */
    public void update(String companyName, String title, LocalDate deadline,
                       String thumbnailUrl, String originalUrl,
                       Set<CareerType> careerTypes, String category, String region, String regionRaw) {
        applyMutableFields(companyName, title, deadline, thumbnailUrl, originalUrl,
                careerTypes, category, region, regionRaw);
    }

    private void applyMutableFields(String companyName, String title, LocalDate deadline,
                                    String thumbnailUrl, String originalUrl,
                                    Set<CareerType> careerTypes, String category, String region, String regionRaw) {
        this.companyName = companyName;
        this.title = title;
        this.deadline = deadline;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.careerTypes.clear();
        if (careerTypes != null) {
            this.careerTypes.addAll(careerTypes);
        }
        this.category = category;
        this.region = region;
        this.regionRaw = regionRaw;
        this.crawledAt = LocalDateTime.now();
    }

}
