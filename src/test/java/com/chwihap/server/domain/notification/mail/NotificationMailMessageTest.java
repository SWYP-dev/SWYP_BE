package com.chwihap.server.domain.notification.mail;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.notification.entity.Notification;
import com.chwihap.server.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMailMessageTest {
    @Test
    void 변경_메일은_기존_디자인의_전용_템플릿으로_여러_공고를_표시한다() {
        NotificationMailMessage message = NotificationMailMessage.jobFeedChanges(
                List.of(notification("첫 회사", "새 회사"), notification("다음 회사", "변경 회사")));

        assertThat(message.subject()).isEqualTo("[취합] 공고 정보 변경 2건 안내");
        assertThat(message.htmlText()).contains("charset=\"UTF-8\"", "cid:chwihap-text-logo",
                "background:#F6F6F7", "원본 공고 변경 2건", "첫 회사", "다음 회사",
                HtmlUtils.htmlEscape("회사명: 첫 회사 → 새 회사"),
                HtmlUtils.htmlEscape("마감일: 2026-10-20 → 미정"), "이메일 수신 여부는 취합 서비스의 알림 설정에서 변경할 수 있습니다.");
        assertThat(message.htmlText()).doesNotContain("{{", "cid:chwihap-date-icon", "마감 임박 공고가");
        assertThat(message.plainText()).contains("첫 회사", "다음 회사");
    }

    @Test
    void 변경값과_공고명의_HTML은_이스케이프하고_긴_링크는_줄바꿈한다() {
        var message = NotificationMailMessage.jobFeedChanges(
                List.of(notification("<회사>&", "<script>회사</script>")));

        assertThat(message.htmlText()).contains("&lt;회사&gt;&amp;", "&lt;script&gt;회사&lt;/script&gt;",
                "table-layout:fixed", "word-break:break-all");
        assertThat(message.htmlText()).doesNotContain("<script>");
    }

    @Test
    void 마감_메일은_기존_템플릿을_그대로_사용한다() {
        LocalDate today = LocalDate.of(2026, 10, 19);
        KanbanCard card = card("기존 회사", LocalDate.of(2026, 10, 20));

        var message = NotificationMailMessage.deadlineDigest(List.of(card), today);

        assertThat(message.subject()).isEqualTo("[취합] 마감 임박 공고 1건 안내");
        assertThat(message.htmlText()).contains("마감 임박 공고가 1건 있어요", "cid:chwihap-date-icon",
                "cid:chwihap-text-logo", "지원 마감일에서 모아보기");
        assertThat(message.htmlText()).doesNotContain("{{", "원본 공고 변경");
    }

    private Notification notification(String company, String nextCompany) {
        LocalDate deadline = LocalDate.of(2026, 10, 20);
        JobFeed feed = JobFeed.create("123", company, "채용 공고", deadline, null, "https://example.com/old",
                JobPlatform.PUBLIC_PERSONNEL, Set.of(), null, null, null);
        JobFeedChange change = JobFeedChange.capture(feed, nextCompany, null,
                "https://example.com/new?a=1&b=2", LocalDateTime.of(2026, 10, 16, 0, 55));
        String text = company + " · 채용 공고\n" + String.join("\n", change.describeDifferences());
        return Notification.jobFeedChanged(null, card(company, deadline), change, NotificationType.EMAIL, text);
    }

    private KanbanCard card(String company, LocalDate deadline) {
        var posting = ApplicationPosting.createDirect(null, company, "채용 공고", deadline, null);
        var card = KanbanCard.createCard(null, posting, 1);
        ReflectionTestUtils.setField(card, "id", 42L);
        return card;
    }
}
