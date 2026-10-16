package com.chwihap.server.domain.feed.entity;

import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JobFeedChangeTest {
    private final LocalDate deadline = LocalDate.of(2026, 10, 20);
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 15, 10, 0);

    @Test
    void 동일한_값을_재수집하면_변경_이력을_만들지_않는다() {
        assertThat(JobFeedChange.capture(feed(), "기존 회사", deadline, "https://example.com/old", now))
                .isNull();
    }

    @Test
    void 세_항목의_변경_전후를_원본_식별자와_함께_보관한다() {
        JobFeedChange change = JobFeedChange.capture(feed(), "새 회사", deadline.plusDays(1),
                "https://example.com/new", now);
        assertThat(change.getPlatform()).isEqualTo(JobPlatform.PUBLIC);
        assertThat(change.getExternalId()).isEqualTo("source-1");
        assertThat(change.getOldCompanyName()).isEqualTo("기존 회사");
        assertThat(change.getNewCompanyName()).isEqualTo("새 회사");
        assertThat(change.getChangedAt()).isEqualTo(now);
        assertThat(change.describeDifferences()).containsExactly(
                "회사명: 기존 회사 → 새 회사", "마감일: 2026-10-20 → 2026-10-21",
                "공고 링크: https://example.com/old → https://example.com/new");
    }

    @Test
    void 사용자가_수정했지만_원본이_바뀌지_않은_항목은_알리지_않는다() {
        JobFeedChange change = JobFeedChange.capture(feed(), "기존 회사", deadline.plusDays(1),
                "https://example.com/old", now);
        ApplicationPosting posting = posting();
        posting.updateDetails("사용자 회사", "제목", deadline, "https://example.com/custom");
        assertThat(change.describeDifferences()).containsExactly("마감일: 2026-10-20 → 2026-10-21");
        assertThat(posting.getCompanyName()).isEqualTo("사용자 회사");
        assertThat(posting.getDeadline()).isEqualTo(deadline);
    }

    @Test
    void 변경된_원본과_저장값이_이미_같아도_알린다() {
        JobFeedChange change = JobFeedChange.capture(feed(), "새 회사", deadline,
                "https://example.com/old", now);
        ApplicationPosting posting = posting();
        posting.updateDetails("새 회사", "제목", deadline, "https://example.com/old");
        assertThat(change.describeDifferences()).containsExactly("회사명: 기존 회사 → 새 회사");
    }

    @Test
    void 마감일과_링크가_null로_바뀌는_경우도_알린다() {
        JobFeedChange change = JobFeedChange.capture(feed(), "기존 회사", null, null, now);
        assertThat(change.describeDifferences()).containsExactly(
                "마감일: 2026-10-20 → 미정", "공고 링크: https://example.com/old → 미정");
    }

    @Test
    void null인_마감일과_링크가_확정되면_알린다() {
        JobFeed feed = JobFeed.create("source-1", "기존 회사", "제목", null, null, null,
                JobPlatform.PUBLIC, Set.of(), null, null, null);
        JobFeedChange change = JobFeedChange.capture(feed, "기존 회사", deadline,
                "https://example.com/new", now);
        ApplicationPosting posting = ApplicationPosting.createDirect(null, "기존 회사", "제목", null, null);
        assertThat(change.describeDifferences()).containsExactly(
                "마감일: 미정 → 2026-10-20", "공고 링크: 미정 → https://example.com/new");
    }

    private JobFeed feed() {
        return JobFeed.create("source-1", "기존 회사", "제목", deadline, null, "https://example.com/old",
                JobPlatform.PUBLIC, Set.of(), null, null, null);
    }

    private ApplicationPosting posting() {
        return ApplicationPosting.createDirect(null, "기존 회사", "제목", deadline, "https://example.com/old");
    }
}
