package com.chwihap.server.domain.notification.mail;

import com.chwihap.server.domain.notification.entity.Notification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/** 기존 마감 메일의 디자인을 유지하는 원본 공고 변경 전용 HTML 템플릿. */
final class JobFeedChangeMailTemplate {
    private static final String TEMPLATE_PATH = "templates/mail/job-feed-change.html";
    private static final String TEMPLATE = loadTemplate();
    private static final String DEADLINES_URL = "https://www.chwihap.com/deadlines";

    private JobFeedChangeMailTemplate() {
    }

    static String render(List<Notification> notifications) {
        String changesHtml = notifications.stream()
                .map(JobFeedChangeMailTemplate::renderCard)
                .collect(Collectors.joining("<div style=\"height:12px; line-height:0; font-size:0;\">&nbsp;</div>"));
        // 사용자 데이터에 템플릿 구문이 포함되어도 재치환되지 않도록 동적 HTML은 마지막에 넣는다.
        return TEMPLATE.replace("{{totalCount}}", String.valueOf(notifications.size()))
                .replace("{{changesHtml}}", changesHtml);
    }

    private static String renderCard(Notification notification) {
        var card = notification.getKanbanCard();
        var posting = card.getApplicationPosting();
        String differences = notification.getJobFeedChange().describeDifferences().stream()
                .map(value -> "<p style=\"margin:8px 0 0; font-size:13px; line-height:1.6; color:#616164; word-break:break-all; overflow-wrap:anywhere;\">"
                        + HtmlUtils.htmlEscape(value) + "</p>")
                .collect(Collectors.joining());
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="width:100%%; table-layout:fixed; background:#FFFFFF; border:1px solid #E9EBEC; border-radius:12px;">
                  <tr>
                    <td style="width:4px; padding:16px 0 16px 16px; vertical-align:top;"><div style="width:4px; height:75px; background:#4864F1; border-radius:1000px; font-size:0; line-height:0;">&nbsp;</div></td>
                    <td style="padding:16px; vertical-align:top; word-break:break-all; overflow-wrap:anywhere;">
                      <p style="margin:0; font-weight:500; font-size:14px; line-height:1.5; color:#616164;">%s</p>
                      <p style="margin:4px 0 0; font-weight:600; font-size:16px; line-height:1.5;"><a href="%s" style="color:#212123; text-decoration:none;">%s</a></p>
                      %s
                    </td>
                  </tr>
                </table>
                """.formatted(HtmlUtils.htmlEscape(posting.getCompanyName()),
                DEADLINES_URL + "?cardId=" + card.getId(), HtmlUtils.htmlEscape(posting.getTitle()), differences);
    }

    private static String loadTemplate() {
        try (var input = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("이메일 템플릿을 읽을 수 없습니다: " + TEMPLATE_PATH, e);
        }
    }
}
