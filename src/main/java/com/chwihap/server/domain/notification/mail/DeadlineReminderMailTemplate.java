package com.chwihap.server.domain.notification.mail;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

final class DeadlineReminderMailTemplate {

    private static final String TEMPLATE_PATH = "templates/mail/deadline-reminder.html";
    private static final String TEMPLATE = loadTemplate();

    private DeadlineReminderMailTemplate() {
    }

    static String render(
            String companyName,
            String postingTitle,
            String formattedDeadline,
            int daysLeft
    ) {
        return TEMPLATE
                .replace("{{daysLeft}}", String.valueOf(daysLeft))
                .replace("{{companyName}}", HtmlUtils.htmlEscape(companyName))
                .replace("{{postingTitle}}", HtmlUtils.htmlEscape(postingTitle))
                .replace("{{deadline}}", HtmlUtils.htmlEscape(formattedDeadline));
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
