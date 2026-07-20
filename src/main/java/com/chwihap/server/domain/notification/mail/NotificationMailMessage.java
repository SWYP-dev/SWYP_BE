package com.chwihap.server.domain.notification.mail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record NotificationMailMessage(
        String subject,
        String plainText,
        String htmlText
) {

    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    public static NotificationMailMessage deadlineReminder(
            String companyName,
            String postingTitle,
            LocalDate deadline,
            int daysLeft
    ) {
        String formattedDeadline = deadline.format(DEADLINE_FORMATTER);
        String subject = "[취합] " + companyName + " 지원 마감 D-" + daysLeft;
        String plainText = """
                안녕하세요. 취합입니다.

                회원님이 취합 칸반에 등록한 채용공고의 지원 마감일이 %d일 남았습니다.

                회사: %s
                공고명: %s
                마감일: %s

                본 메일은 회원님이 설정한 채용공고 마감 알림입니다.
                이메일 알림은 취합 서비스의 알림 설정에서 변경할 수 있습니다.

                취합 드림
                """.formatted(daysLeft, companyName, postingTitle, formattedDeadline);

        String htmlText = DeadlineReminderMailTemplate.render(
                companyName,
                postingTitle,
                formattedDeadline,
                daysLeft
        );

        return new NotificationMailMessage(subject, plainText, htmlText);
    }
}
