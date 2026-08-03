package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.notification.dto.InAppNotificationReadRequest;
import com.chwihap.server.domain.notification.dto.NotificationSettingUpdateRequest;
import com.chwihap.server.domain.notification.entity.Notification;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Test
    void 인앱_알림_조회_크기를_생략하면_기본_10개에_다음_항목_확인용_1개를_더_조회한다() {
        given(notificationRepository.findInbox(
                1L,
                NotificationType.IN_APP,
                null,
                PageRequest.of(0, 11)
        )).willReturn(List.of());

        var response = notificationService.getInbox(1L, null, null);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        verify(notificationRepository).findInbox(
                1L,
                NotificationType.IN_APP,
                null,
                PageRequest.of(0, 11)
        );
    }

    @Test
    void 인앱_알림은_생성_당시_D_Day와_공고_정보를_화면용_필드로_반환한다() {
        LocalDate deadline = LocalDate.of(2026, 7, 15);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 14, 9, 0);
        User user = user(1L);
        KanbanCard card = card(user, 10L, deadline);
        Notification notification = Notification.inApp(
                user,
                card,
                "와탭랩스 지원 마감 D-1입니다.",
                1
        );
        ReflectionTestUtils.setField(notification, "id", 5L);
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);

        given(notificationRepository.findInbox(
                1L,
                NotificationType.IN_APP,
                null,
                PageRequest.of(0, 11)
        )).willReturn(List.of(notification));
        given(notificationRepository.countByUser_IdAndTypeAndIsReadFalse(1L, NotificationType.IN_APP))
                .willReturn(1L);

        var response = notificationService.getInbox(1L, null, null);

        assertThat(response.unreadCount()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(5L);
            assertThat(item.cardId()).isEqualTo(10L);
            assertThat(item.dDayLabel()).isEqualTo("D-1");
            assertThat(item.title()).isEqualTo("와탭랩스 공고가 하루 남았어요 ⏰");
            assertThat(item.description())
                    .isEqualTo("Java/Spring Boot 백엔드 개발자 채용 · 아직 지원 전이라면 서둘러 주세요!");
            assertThat(item.createdAt()).isEqualTo(createdAt);
        });
    }

    private KanbanCard card(User user, Long cardId, LocalDate deadline) {
        ApplicationPosting posting = ApplicationPosting.createDirect(
                user,
                "와탭랩스",
                "Java/Spring Boot 백엔드 개발자 채용",
                deadline,
                "https://example.com"
        );
        KanbanStage stage = KanbanStage.kanbanDefault(user, "지원 전", 1);
        KanbanCard card = KanbanCard.createCard(stage, posting, 1);
        ReflectionTestUtils.setField(card, "id", cardId);
        return card;
    }

    private User user(Long id) {
        User user = User.create("user@example.com", "사용자", null, AuthProvider.KAKAO, "kakao-id");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
