package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.notification.dto.InAppNotificationReadRequest;
import com.chwihap.server.domain.notification.dto.NotificationSettingUpdateRequest;
import com.chwihap.server.domain.notification.entity.NotificationSetting;
import com.chwihap.server.domain.notification.enums.NotificationType;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.notification.repository.NotificationSettingRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void 알림_설정이_없으면_명세의_기본값으로_생성해_조회한다() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationSettingRepository.findByUser_Id(1L)).willReturn(Optional.empty());
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = notificationService.getSettings(1L);

        assertThat(response.emailEnabled()).isTrue();
        assertThat(response.inAppEnabled()).isTrue();
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.remindDays()).containsExactly(7, 3, 1, 0);
        verify(notificationSettingRepository).save(any(NotificationSetting.class));
    }

    @Test
    void 알림_설정_수정시_리마인드_기준일을_중복_제거하고_내림차순으로_정렬한다() {
        User user = user(1L);
        NotificationSetting setting = NotificationSetting.createDefault(user);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationSettingRepository.findByUser_Id(1L)).willReturn(Optional.of(setting));

        var response = notificationService.updateSettings(
                1L,
                new NotificationSettingUpdateRequest(false, true, List.of(1, 7, 3, 7))
        );

        assertThat(response.emailEnabled()).isFalse();
        assertThat(response.inAppEnabled()).isTrue();
        assertThat(response.remindDays()).containsExactly(7, 3, 1);
    }

    @Test
    void 허용되지_않은_리마인드_기준일은_거부한다() {
        assertThatThrownBy(() -> notificationService.updateSettings(
                1L,
                new NotificationSettingUpdateRequest(true, true, List.of(5))
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 리마인드_기준일에_0을_포함하면_D_Day도_허용된다() {
        User user = user(1L);
        NotificationSetting setting = NotificationSetting.createDefault(user);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationSettingRepository.findByUser_Id(1L)).willReturn(Optional.of(setting));

        var response = notificationService.updateSettings(
                1L,
                new NotificationSettingUpdateRequest(true, true, List.of(0, 7, 3, 1))
        );

        assertThat(response.remindDays()).containsExactly(7, 3, 1, 0);
    }

    @Test
    void 인앱_읽음_처리는_중복_ID를_제거하고_실제_수정_건수를_반환한다() {
        given(notificationRepository.markAsRead(1L, NotificationType.IN_APP, List.of(4L, 5L)))
                .willReturn(2);

        var response = notificationService.readInbox(
                1L,
                new InAppNotificationReadRequest(List.of(4L, 5L, 4L))
        );

        assertThat(response.updatedCount()).isEqualTo(2);
        verify(notificationRepository).markAsRead(1L, NotificationType.IN_APP, List.of(4L, 5L));
    }

    private User user(Long id) {
        User user = User.create("user@example.com", "사용자", null, AuthProvider.KAKAO, "kakao-id");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
