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
                groupsHtml.append("<div style=\"height:32px; line-height:0; font-size:0;\">&nbsp;</div>");
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
            default -> cards.get(0).getApplicationPosting().getDeadline().format(GROUP_DEADLINE_FORMATTER);
        };
        String accentColor = (daysLeft == 0 || daysLeft == 1)
                ? TODAY_TOMORROW_ACCENT_COLOR
                : OTHER_DEADLINE_ACCENT_COLOR;
        String badgeColor = daysLeft >= 3 ? BADGE_MUTED_COLOR : TODAY_TOMORROW_ACCENT_COLOR;

        StringBuilder cardsHtml = new StringBuilder();
        boolean firstCard = true;
        for (KanbanCard card : cards) {
            if (!firstCard) {
                cardsHtml.append("<div style=\"height:12px; line-height:0; font-size:0;\">&nbsp;</div>");
            }
            firstCard = false;
            cardsHtml.append(renderCard(card, accentColor));
        }

        return """
                <div>
                  <div style="margin:0 0 16px;">
                    <span style="font-weight:600; font-size:18px; line-height:1.4; color:%s;">%s</span>
                    <span style="padding-left:6px; font-weight:500; font-size:16px; line-height:1.5; color:%s;">%s</span>
                  </div>
                  %s
                </div>
                """.formatted(accentColor, deadlineLabel, badgeColor, badgeLabel, cardsHtml);
    }

    private static String renderCard(KanbanCard card, String accentColor) {
        String companyName = HtmlUtils.htmlEscape(card.getApplicationPosting().getCompanyName());
        String postingTitle = HtmlUtils.htmlEscape(card.getApplicationPosting().getTitle());
        String deadline = card.getApplicationPosting().getDeadline().format(CARD_DEADLINE_FORMATTER);

        return """
                <a href="%s" style="display:block; text-decoration:none; color:#212123;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="width:100%%; background:#FFFFFF; border:1px solid #E9EBEC; border-radius:12px;">
                  <tr>
                    <td style="width:4px; padding:12px 0 12px 16px;"><div style="width:4px; height:75px; background:%s; border-radius:1000px; font-size:0; line-height:0;">&nbsp;</div></td>
                    <td style="padding:16px; vertical-align:middle;">
                      <p style="margin:0; font-weight:500; font-size:14px; line-height:1.5; color:#616164;">%s</p>
                      <p style="margin:4px 0 0; font-weight:600; font-size:16px; line-height:1.5; color:#212123;">%s</p>
                      <p style="margin:8px 0 0; font-weight:500; font-size:12px; line-height:1.5; color:#9E9EA1;"><img src="cid:chwihap-date-icon" width="14" height="14" alt="" style="vertical-align:middle;">&nbsp; ~ %s</p>
                    </td>
                    <td style="width:18px; padding:12px 16px 12px 0; vertical-align:middle; text-align:right;"><span style="font-size:18px; line-height:1; color:#9E9EA1;">&rsaquo;</span></td>
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
