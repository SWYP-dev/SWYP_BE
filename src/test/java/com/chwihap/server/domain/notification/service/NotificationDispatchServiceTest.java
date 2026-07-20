package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.notification.entity.Notification;
import com.chwihap.server.domain.notification.entity.NotificationSetting;
import com.chwihap.server.domain.notification.enums.NotificationStatus;
import com.chwihap.server.domain.notification.enums.NotificationType;
import com.chwihap.server.domain.notification.mail.NotificationMailMessage;
import com.chwihap.server.domain.notification.mail.NotificationMailSender;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.notification.repository.NotificationSettingRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private KanbanCardRepository kanbanCardRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMailSender notificationMailSender;

    @InjectMocks
    private NotificationDispatchService notificationDispatchService;

    @Test
    void 설정이_없으면_D3_카드에_이메일과_인앱_알림을_모두_발송한다() {
        LocalDate today = LocalDate.of(2026, 7, 20);
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 9, 0);
        KanbanCard card = card(1L, 10L, today.plusDays(3));
        given(kanbanCardRepository.findDeadlineReminderTargets(any())).willReturn(List.of(card));
        given(notificationSettingRepository.findByUser_IdIn(any())).willReturn(List.of());
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        notificationDispatchService.dispatch(today, now);

        ArgumentCaptor<NotificationMailMessage> mailCaptor =
                ArgumentCaptor.forClass(NotificationMailMessage.class);
        verify(notificationMailSender).send(eq("user@example.com"), mailCaptor.capture());
        assertThat(mailCaptor.getValue().subject()).isEqualTo("[취합] 카카오 지원 마감 D-3");
        assertThat(mailCaptor.getValue().plainText())
                .contains("회사: 카카오", "공고명: 백엔드 개발자", "마감일: 2026년 7월 23일");
        assertThat(mailCaptor.getValue().htmlText()).contains("지원 마감 D-3");
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getType)
                .containsExactly(NotificationType.EMAIL, NotificationType.IN_APP);
    }

    @Test
    void 사용자_설정에서_채널과_기준일이_꺼져있으면_알림을_생성하지_않는다() {
        LocalDate today = LocalDate.of(2026, 7, 20);
        User user = user(1L);
        KanbanCard card = card(user, 10L, today.plusDays(3));
        NotificationSetting setting = NotificationSetting.createDefault(user);
        setting.update(false, true, List.of(1));
        given(kanbanCardRepository.findDeadlineReminderTargets(any())).willReturn(List.of(card));
        given(notificationSettingRepository.findByUser_IdIn(any())).willReturn(List.of(setting));

        notificationDispatchService.dispatch(today, today.atTime(9, 0));

        verify(notificationMailSender, never()).send(any(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void 이메일_발송이_실패해도_FAILED_이력을_저장하고_인앱_알림은_생성한다() {
        LocalDate today = LocalDate.of(2026, 7, 20);
        LocalDateTime now = today.atTime(9, 0);
        KanbanCard card = card(1L, 10L, today.plusDays(1));
        given(kanbanCardRepository.findDeadlineReminderTargets(any())).willReturn(List.of(card));
        given(notificationSettingRepository.findByUser_IdIn(any())).willReturn(List.of());
        doThrow(new IllegalStateException("SMTP 오류"))
                .when(notificationMailSender).send(any(), any());

        notificationDispatchService.dispatch(today, now);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        Notification emailHistory = captor.getAllValues().get(0);
        assertThat(emailHistory.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(emailHistory.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getAllValues().get(1).getType()).isEqualTo(NotificationType.IN_APP);
    }

    private KanbanCard card(Long userId, Long cardId, LocalDate deadline) {
        return card(user(userId), cardId, deadline);
    }

    private KanbanCard card(User user, Long cardId, LocalDate deadline) {
        JobPosting posting = JobPosting.createDirect(
                user, "카카오", "백엔드 개발자", deadline, "https://example.com");
        KanbanStage stage = KanbanStage.kanbanDefault(user, "지원 전", 1);
        KanbanCard card = KanbanCard.createCard(user, stage, posting, 1);
        ReflectionTestUtils.setField(card, "id", cardId);
        return card;
    }

    private User user(Long id) {
        User user = User.create("user@example.com", "사용자", null, AuthProvider.KAKAO, "kakao-id");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
