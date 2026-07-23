package com.chwihap.server.domain.notification.mail;

import com.chwihap.server.domain.kanban.entity.KanbanCard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record NotificationMailMessage(
        String subject,
        String plainText,
        String htmlText
) {

    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    public static NotificationMailMessage deadlineDigest(List<KanbanCard> cards, LocalDate today) {
        Map<Integer, List<KanbanCard>> groupedByDaysLeft = cards.stream()
                .collect(Collectors.groupingBy(
                        card -> (int) ChronoUnit.DAYS.between(today, card.getJobPosting().getDeadline()),
                        TreeMap::new,
                        Collectors.toList()
                ));

        int totalCount = cards.size();
        String summaryText = groupedByDaysLeft.entrySet().stream()
                .map(entry -> summaryLabel(entry.getKey()) + " 마감 " + entry.getValue().size() + "건")
                .collect(Collectors.joining(" · "));

        String subject = "[취합] 마감 임박 공고 " + totalCount + "건 안내";
        String plainText = buildPlainText(groupedByDaysLeft, totalCount, summaryText);
        String htmlText = DeadlineDigestMailTemplate.render(groupedByDaysLeft, totalCount, summaryText);

        return new NotificationMailMessage(subject, plainText, htmlText);
    }

    private static String buildPlainText(
            Map<Integer, List<KanbanCard>> groupedByDaysLeft,
            int totalCount,
            String summaryText
    ) {
        StringBuilder plainText = new StringBuilder("""
                안녕하세요. 취합입니다.

                회원님이 취합 칸반에 등록한 채용공고 중 마감이 임박한 공고가 %d건 있어요. (%s)

                """.formatted(totalCount, summaryText));

        groupedByDaysLeft.forEach((daysLeft, groupCards) -> {
            plainText.append(summaryLabel(daysLeft)).append("\n");
            for (KanbanCard card : groupCards) {
                plainText.append(" - ")
                        .append(card.getJobPosting().getCompanyName())
                        .append(" · ")
                        .append(card.getJobPosting().getTitle())
                        .append(" (~ ")
                        .append(card.getJobPosting().getDeadline().format(DEADLINE_FORMATTER))
                        .append(")\n");
            }
            plainText.append("\n");
        });

        plainText.append("""
                본 메일은 회원님이 설정한 채용공고 마감 알림입니다.
                이메일 알림은 취합 서비스의 알림 설정에서 변경할 수 있습니다.

                취합 드림
                """);

        return plainText.toString();
    }

    private static String summaryLabel(int daysLeft) {
        if (daysLeft == 0) {
            return "오늘";
        }
        return daysLeft == 1 ? "내일" : "D-" + daysLeft;
    }

}
