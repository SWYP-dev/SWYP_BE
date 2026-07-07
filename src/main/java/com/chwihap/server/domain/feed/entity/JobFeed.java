package com.chwihap.server.domain.feed.entity;

import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CareerType careerType;

    @Column(nullable = true, length = 50)
    private String category;

    @Column(nullable = true, length = 100)
    private String region;

    @Column(nullable = false)
    private LocalDateTime crawledAt;

    public static JobFeed create(String externalId, String companyName, String title, LocalDate deadline,
                                  String thumbnailUrl, String originalUrl, JobPlatform platform,
                                  CareerType careerType, String category, String region) {
        JobFeed feed = new JobFeed();
        feed.externalId = externalId;
        feed.companyName = companyName;
        feed.title = title;
        feed.deadline = deadline;
        feed.thumbnailUrl = thumbnailUrl;
        feed.originalUrl = originalUrl;
        feed.platform = platform;
        feed.careerType = careerType;
        feed.category = category;
        feed.region = region;
        feed.crawledAt = LocalDateTime.now();
        return feed;
    }

}
