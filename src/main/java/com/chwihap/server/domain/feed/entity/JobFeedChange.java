package com.chwihap.server.domain.feed.entity;

import com.chwihap.server.domain.feed.enums.JobPlatform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "job_feed_changes", indexes = {
        @Index(name = "idx_job_feed_changes_pending", columnList = "processed_at, changed_at, id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobFeedChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPlatform platform;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String oldCompanyName;

    @Column(nullable = false)
    private String newCompanyName;

    private LocalDate oldDeadline;

    private LocalDate newDeadline;

    @Column(length = 1000)
    private String oldOriginalUrl;

    @Column(length = 1000)
    private String newOriginalUrl;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    private LocalDateTime processedAt;

    /** 새 공고(공고 크롤링)와 원본 공고(job_feed)에서 바뀐 데이터가 있으면 감지해서 테이블에 저장한다.*/
    public static JobFeedChange capture(JobFeed feed, String companyName, LocalDate deadline,
                                        String originalUrl, LocalDateTime now) {
        if (Objects.equals(feed.getCompanyName(), companyName)
                && Objects.equals(feed.getDeadline(), deadline)
                && Objects.equals(feed.getOriginalUrl(), originalUrl)) {
            return null;
        }
        JobFeedChange change = new JobFeedChange();
        change.platform = feed.getPlatform();
        change.externalId = feed.getExternalId();
        change.oldCompanyName = feed.getCompanyName();
        change.newCompanyName = companyName;
        change.oldDeadline = feed.getDeadline();
        change.newDeadline = deadline;
        change.oldOriginalUrl = feed.getOriginalUrl();
        change.newOriginalUrl = originalUrl;
        change.changedAt = now;
        return change;
    }

    /** 사용자 저장값과 관계없이 원본 공고에서 바뀐 항목을 안내한다. */
    public List<String> describeDifferences() {
        List<String> details = new ArrayList<>();
        addDifference(details, "회사명", oldCompanyName, newCompanyName);
        addDifference(details, "마감일", oldDeadline, newDeadline);
        addDifference(details, "공고 링크", oldOriginalUrl, newOriginalUrl);
        return List.copyOf(details);
    }

    /** 바뀐 공고의 내용을 리스트에 저장 */
    private void addDifference(List<String> details, String label, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            details.add(label + ": " + display(before) + " → " + display(after));
        }
    }

    private String display(Object value) {
        return value == null ? "미정" : value.toString();
    }

    public void markProcessed(LocalDateTime now) {
        this.processedAt = now;
    }
}
