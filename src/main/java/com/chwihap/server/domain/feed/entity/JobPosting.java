package com.chwihap.server.domain.feed.entity;

import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "job_postings",
        indexes = {
                @Index(
                        name = "idx_job_posting_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_job_posting_deadline",
                        columnList = "deadline"
                ),
                @Index(
                        name = "idx_job_posting_source",
                        columnList = "source_platform, source_external_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPosting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
    private JobPlatform platform = JobPlatform.DIRECT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CareerType careerType;

    @Column(nullable = true, length = 50)
    private String category;

    @Column(nullable = true, length = 100)
    private String region;

    /**
     * 즐겨찾기 시 복사해온 원본 {@code job_feed}를 역으로 식별하기 위한 컬럼.
     * job_feed row 자체는 만료·재수집으로 사라질 수 있어 FK로 걸지 않고, (platform, externalId) 매칭용으로만 사용한다.
     * 즐겨찾기가 아닌 직접 등록(DIRECT) 공고는 null.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_platform", nullable = true)
    private JobPlatform sourcePlatform;

    @Column(name = "source_external_id", nullable = true)
    private String sourceExternalId;

    public static JobPosting copyFromFeed(JobFeed feed, User user) {
        JobPosting posting = new JobPosting();
        posting.user = user;
        posting.companyName = feed.getCompanyName();
        posting.title = feed.getTitle();
        posting.deadline = feed.getDeadline();
        posting.thumbnailUrl = feed.getThumbnailUrl();
        posting.originalUrl = feed.getOriginalUrl();
        posting.platform = feed.getPlatform();
        posting.careerType = feed.getCareerType();
        posting.category = feed.getCategory();
        posting.region = feed.getRegion();
        posting.sourcePlatform = feed.getPlatform();
        posting.sourceExternalId = feed.getExternalId();
        return posting;
    }

}
