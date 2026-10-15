package com.chwihap.server.domain.feed.entity;

import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
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

    /** 원본에서 바뀌었고 사용자의 저장값과도 다른 항목만 안내한다. */
    public List<String> describeDifferences(ApplicationPosting posting) {
        List<String> details = new ArrayList<>();
        addDifference(details, "회사명", oldCompanyName, newCompanyName, posting.getCompanyName());
        addDifference(details, "마감일", oldDeadline, newDeadline, posting.getDeadline());
        addDifference(details, "공고 링크", oldOriginalUrl, newOriginalUrl, posting.getOriginalUrl());
        return List.copyOf(details);
    }

    private void addDifference(List<String> details, String label, Object before, Object after, Object saved) {
        if (!Objects.equals(before, after) && !Objects.equals(saved, after)) {
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
