package com.chwihap.server.domain.kanban.entity;

import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(
        name = "application_postings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_application_postings_user_source",
                        columnNames = {"user_id", "source_job_posting_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_application_postings_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_application_postings_source",
                        columnList = "source_job_posting_id"
                ),
                @Index(
                        name = "idx_application_postings_deadline",
                        columnList = "deadline"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationPosting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "source_job_posting_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "fk_application_postings_source")
    )
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private JobPosting sourceJobPosting;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true)
    private LocalDate deadline;

    @Column(nullable = true, length = 500)
    private String thumbnailUrl;

    @Column(nullable = true, length = 2048)
    private String originalUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'DIRECT'")
    private JobPlatform platform = JobPlatform.DIRECT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CareerType careerType;

    @Column(nullable = true, length = 50)
    private String category;

    @Column(nullable = true, length = 100)
    private String region;

    // 직접 생성할 경우(추가로 값을 받아야하는 경우 필드 추가)
    public static ApplicationPosting createDirect(
            User user,
            String companyName,
            String title,
            LocalDate deadline,
            String originalUrl
    ) {
        ApplicationPosting posting = new ApplicationPosting();
        posting.user = user;
        posting.companyName = companyName;
        posting.title = title;
        posting.deadline = deadline;
        posting.originalUrl = originalUrl;
        posting.platform = JobPlatform.DIRECT;
        return posting;
    }

    // 공고 자동 생성할 경우 사용
    public static ApplicationPosting copyFromJobPosting(JobPosting sourceJobPosting) {
        ApplicationPosting posting = new ApplicationPosting();
        posting.user = sourceJobPosting.getUser();
        posting.sourceJobPosting = sourceJobPosting;
        posting.companyName = sourceJobPosting.getCompanyName();
        posting.title = sourceJobPosting.getTitle();
        posting.deadline = sourceJobPosting.getDeadline();
        posting.thumbnailUrl = sourceJobPosting.getThumbnailUrl();
        posting.originalUrl = sourceJobPosting.getOriginalUrl();
        posting.platform = sourceJobPosting.getPlatform();
        posting.careerType = sourceJobPosting.getCareerType();
        posting.category = sourceJobPosting.getCategory();
        posting.region = sourceJobPosting.getRegion();
        return posting;
    }

    // 공고의 세부 내용을 수정할 때 사용(추가로 값을 받아야하는 경우 필드 추가)
    public void updateDetails(
            String companyName,
            String title,
            LocalDate deadline,
            String originalUrl
    ) {
        this.companyName = companyName;
        this.title = title;
        this.deadline = deadline;
        this.originalUrl = originalUrl;
    }

    public void updateDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
