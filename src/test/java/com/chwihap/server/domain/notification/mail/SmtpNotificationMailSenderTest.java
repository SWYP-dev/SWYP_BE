package com.chwihap.server.domain.notification.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpNotificationMailSenderTest {

    @Mock
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @Test
    void 일반_텍스트와_HTML을_포함한_MIME_이메일을_전송한다() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
        SmtpNotificationMailSender mailSender = new SmtpNotificationMailSender(
                javaMailSender,
                "notification@chwihap.com",
                "취합"
        );
        NotificationMailMessage message = new NotificationMailMessage(
                "[취합] 테스트 지원 마감 D-1",
                "일반 텍스트 본문",
                "<html><body><strong>HTML 본문</strong></body></html>"
        );

        mailSender.send("user@example.com", message);

        verify(javaMailSender).send(mimeMessage);
        InternetAddress from = (InternetAddress) mimeMessage.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("notification@chwihap.com");
        assertThat(from.getPersonal()).isEqualTo("취합");
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("[취합] 테스트 지원 마감 D-1");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mimeMessage.writeTo(output);
        String rawMessage = output.toString(StandardCharsets.UTF_8);
        assertThat(rawMessage).contains("Content-Type: text/plain", "Content-Type: text/html");
    }
}
