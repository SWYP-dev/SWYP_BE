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

}
