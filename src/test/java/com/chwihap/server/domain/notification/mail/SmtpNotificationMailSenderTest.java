package com.chwihap.server.domain.notification.mail;

import jakarta.mail.Session;
import jakarta.mail.Part;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpNotificationMailSenderTest {

    @Mock
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @Test
    void 한글_제목과_본문은_UTF8로_왕복하고_참조한_이미지만_첨부한다() throws Exception {
        var session = Session.getInstance(new Properties());
        var mimeMessage = new MimeMessage(session);
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
        var message = new NotificationMailMessage("[취합] 공고 정보 변경 2건 안내",
                "회사명: 기존 회사 → 새 회사", "<html lang=\"ko\"><body><img src=\"cid:chwihap-text-logo\">회사명: 기존 회사 → 새 회사</body></html>");

        new SmtpNotificationMailSender(javaMailSender, "notification@chwihap.com", "취합")
                .send("user@example.com", message);
        var output = new ByteArrayOutputStream();
        mimeMessage.writeTo(output);
        var decoded = new MimeMessage(session, new ByteArrayInputStream(output.toByteArray()));
        var parts = new ArrayList<Part>();
        collectParts(decoded, parts);

        assertThat(decoded.getSubject()).isEqualTo(message.subject());
        int textParts = 0;
        int imageParts = 0;
        for (Part part : parts) {
            if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                textParts++;
                assertThat(part.getContentType()).containsIgnoringCase("charset=UTF-8");
                assertThat((String) part.getContent()).contains("회사명: 기존 회사 → 새 회사");
            }
            if (part.isMimeType("image/png")) {
                imageParts++;
                assertThat(part.getHeader("Content-ID")).containsExactly("<chwihap-text-logo>");
            }
        }
        assertThat(textParts).isEqualTo(2);
        assertThat(imageParts).isEqualTo(1);
    }

    private void collectParts(Part part, List<Part> parts) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                collectParts(multipart.getBodyPart(i), parts);
            }
        } else {
            parts.add(part);
        }
    }

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
