package com.chwihap.server.domain.notification.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Component
public class SmtpNotificationMailSender implements NotificationMailSender {

    private final JavaMailSender javaMailSender;
    private final String sender;
    private final String senderName;

    public SmtpNotificationMailSender(
            JavaMailSender javaMailSender,
            @Value("${app.notification.mail.from-address:}") String sender,
            @Value("${app.notification.mail.from-name:취합}") String senderName
    ) {
        this.javaMailSender = javaMailSender;
        this.sender = sender;
        this.senderName = senderName;
    }

    @Override
    public void send(String recipient, NotificationMailMessage message) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            if (StringUtils.hasText(sender)) {
                if (StringUtils.hasText(senderName)) {
                    helper.setFrom(sender, senderName);
                } else {
                    helper.setFrom(sender);
                }
            }
            helper.setTo(recipient);
            helper.setSubject(message.subject());
            helper.setText(message.plainText(), message.htmlText());
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("알림 이메일 생성에 실패했습니다.", e);
        }
        javaMailSender.send(mimeMessage);
    }
}
