package com.chwihap.server.domain.notification.mail;

import com.chwihap.server.domain.kanban.entity.KanbanCard;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

final class DeadlineDigestMailTemplate {

    private static final String TEMPLATE_PATH = "templates/mail/deadline-reminder.html";
    private static final String TEMPLATE = loadTemplate();
    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("M.d");
    private static final String DEADLINES_URL = "https://www.chwihap.com/deadlines";

    private DeadlineDigestMailTemplate() {
    }

    static String render(Map<Integer, List<KanbanCard>> groupedByDaysLeft, int totalCount, String summaryText) {
        StringBuilder groupsHtml = new StringBuilder();
        boolean firstGroup = true;
        for (Map.Entry<Integer, List<KanbanCard>> entry : groupedByDaysLeft.entrySet()) {
            if (!firstGroup) {
                groupsHtml.append("<div class=\"group-spacer\">&nbsp;</div>");
            }
            firstGroup = false;
            groupsHtml.append(renderGroup(entry.getKey(), entry.getValue()));
        }

        return TEMPLATE
                .replace("{{totalCount}}", String.valueOf(totalCount))
                .replace("{{summaryText}}", HtmlUtils.htmlEscape(summaryText))
                .replace("{{groupsHtml}}", groupsHtml.toString());
    }

    private static String renderGroup(int daysLeft, List<KanbanCard> cards) {
        String primaryLabel = daysLeft == 1 ? "내일" : "마감임박";
        String badgeLabel = daysLeft == 1 ? "D-1" : "D-" + daysLeft;

        StringBuilder cardsHtml = new StringBuilder();
        boolean firstCard = true;
        for (KanbanCard card : cards) {
            if (!firstCard) {
                cardsHtml.append("<div class=\"card-spacer\">&nbsp;</div>");
            }
            firstCard = false;
            cardsHtml.append(renderCard(card));
        }

        return """
                <div>
                  <div class="group-header">
                    <span class="group-label">%s</span>
                    <span class="group-badge">%s</span>
                  </div>
                  %s
                </div>
                """.formatted(primaryLabel, badgeLabel, cardsHtml);
    }

    private static String renderCard(KanbanCard card) {
        String companyName = HtmlUtils.htmlEscape(card.getJobPosting().getCompanyName());
        String postingTitle = HtmlUtils.htmlEscape(card.getJobPosting().getTitle());
        String deadline = card.getJobPosting().getDeadline().format(DEADLINE_FORMATTER);

        return """
                <a href="%s" style="display:block; text-decoration:none; color:inherit;">
                <table role="presentation" class="deadline-card" width="100%%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td class="deadline-card-divider-cell"><div class="deadline-card-divider">&nbsp;</div></td>
                    <td class="deadline-card-content">
                      <p class="deadline-card-company">%s</p>
                      <p class="deadline-card-title">%s</p>
                      <p class="deadline-card-date"><img src="cid:chwihap-date-icon" width="14" height="14" alt="" style="vertical-align:middle;">&nbsp; ~ %s</p>
                    </td>
                    <td class="deadline-card-chevron-cell"><span class="deadline-card-chevron">&rsaquo;</span></td>
                  </tr>
                </table>
                </a>
                """.formatted(DEADLINES_URL, companyName, postingTitle, deadline);
    }

    private static String loadTemplate() {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("이메일 템플릿을 읽을 수 없습니다: " + TEMPLATE_PATH, e);
        }
    }
}
