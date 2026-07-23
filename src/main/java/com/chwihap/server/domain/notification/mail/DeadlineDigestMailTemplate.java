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
import java.util.Locale;
import java.util.Map;

final class DeadlineDigestMailTemplate {

    private static final String TEMPLATE_PATH = "templates/mail/deadline-reminder.html";
    private static final String TEMPLATE = loadTemplate();
    private static final DateTimeFormatter GROUP_DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일");
    private static final DateTimeFormatter CARD_DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("M.d(E)", Locale.KOREAN);
    private static final String DEADLINES_URL = "https://www.chwihap.com/deadlines";
    private static final String TODAY_TOMORROW_ACCENT_COLOR = "#4864F1";
    private static final String OTHER_DEADLINE_ACCENT_COLOR = "#212123";
    private static final String BADGE_MUTED_COLOR = "#616164";

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
        String badgeLabel = daysLeft == 0 ? "D-Day" : "D-" + daysLeft;
        String deadlineLabel = switch (daysLeft) {
            case 0 -> "오늘";
            case 1 -> "내일";
            default -> cards.get(0).getJobPosting().getDeadline().format(GROUP_DEADLINE_FORMATTER);
        };
        String accentColor = (daysLeft == 0 || daysLeft == 1)
                ? TODAY_TOMORROW_ACCENT_COLOR
                : OTHER_DEADLINE_ACCENT_COLOR;
        String badgeColor = daysLeft >= 3 ? BADGE_MUTED_COLOR : TODAY_TOMORROW_ACCENT_COLOR;

        StringBuilder cardsHtml = new StringBuilder();
        boolean firstCard = true;
        for (KanbanCard card : cards) {
            if (!firstCard) {
                cardsHtml.append("<div class=\"card-spacer\">&nbsp;</div>");
            }
            firstCard = false;
            cardsHtml.append(renderCard(card, accentColor));
        }

        return """
                <div>
                  <div class="group-header">
                    <span class="group-label" style="color:%s;">%s</span>
                    <span class="group-badge" style="color:%s;">%s</span>
                  </div>
                  %s
                </div>
                """.formatted(accentColor, deadlineLabel, badgeColor, badgeLabel, cardsHtml);
    }

    private static String renderCard(KanbanCard card, String accentColor) {
        String companyName = HtmlUtils.htmlEscape(card.getJobPosting().getCompanyName());
        String postingTitle = HtmlUtils.htmlEscape(card.getJobPosting().getTitle());
        String deadline = card.getJobPosting().getDeadline().format(CARD_DEADLINE_FORMATTER);

        return """
                <a href="%s" style="display:block; text-decoration:none; color:inherit;">
                <table role="presentation" class="deadline-card" width="100%%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td class="deadline-card-divider-cell"><div class="deadline-card-divider" style="background:%s;">&nbsp;</div></td>
                    <td class="deadline-card-content">
                      <p class="deadline-card-company">%s</p>
                      <p class="deadline-card-title">%s</p>
                      <p class="deadline-card-date"><img src="cid:chwihap-date-icon" width="14" height="14" alt="" style="vertical-align:middle;">&nbsp; ~ %s</p>
                    </td>
                    <td class="deadline-card-chevron-cell"><span class="deadline-card-chevron">&rsaquo;</span></td>
                  </tr>
                </table>
                </a>
                """.formatted(DEADLINES_URL, accentColor, companyName, postingTitle, deadline);
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
