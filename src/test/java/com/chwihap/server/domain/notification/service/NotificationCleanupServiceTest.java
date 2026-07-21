package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    void 생성된_지_90일이_지난_알림을_삭제한다() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 3, 0);
        LocalDateTime threshold = LocalDateTime.of(2026, 4, 22, 3, 0);
        given(notificationRepository.deleteByCreatedAtBefore(threshold)).willReturn(12);
        NotificationCleanupService service = new NotificationCleanupService(notificationRepository);

        int deletedCount = service.deleteExpiredNotifications(now, 90);

        assertThat(deletedCount).isEqualTo(12);
        verify(notificationRepository).deleteByCreatedAtBefore(threshold);
    }

    @Test
    void 보존_기간이_1일보다_짧으면_삭제하지_않는다() {
        NotificationCleanupService service = new NotificationCleanupService(notificationRepository);

        assertThatThrownBy(() -> service.deleteExpiredNotifications(LocalDateTime.now(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 보존 기간은 1일 이상이어야 합니다.");
    }
}
