package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobFeedChangeRepository;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.notification.entity.Notification;
import com.chwihap.server.domain.notification.entity.NotificationSetting;
import com.chwihap.server.domain.notification.enums.NotificationReason;
import com.chwihap.server.domain.notification.enums.NotificationStatus;
import com.chwihap.server.domain.notification.enums.NotificationType;
import com.chwihap.server.domain.notification.mail.NotificationMailMessage;
import com.chwihap.server.domain.notification.mail.NotificationMailSender;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.notification.repository.NotificationSettingRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobFeedChangeDispatchServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 9, 0);
    @Mock private JobFeedChangeRepository changes;
    @Mock private KanbanCardRepository cards;
    @Mock private NotificationRepository notifications;
    @Mock private NotificationSettingRepository settings;
    @Mock private NotificationMailSender mailSender;
    @Mock private PlatformTransactionManager transactionManager;
    private JobFeedChangeDispatchService service;
    private final List<Notification> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new JobFeedChangeDispatchService(changes, cards, notifications, settings,
                mailSender, transactionManager);
        when(transactionManager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        lenient().when(notifications.save(any())).thenAnswer(i -> {
            Notification notification = i.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", (long) saved.size() + 1);
            saved.add(notification);
            return notification;
        });
        lenient().when(notifications.findAllById(any())).thenAnswer(i -> {
            List<Long> ids = i.getArgument(0);
            return saved.stream().filter(n -> ids.contains(n.getId())).toList();
        });
    }

    @Test
    void 네_플랫폼의_변경도_같은_사용자에게_메일_한통으로_통합한다() {
        List<JobPlatform> platforms = List.of(JobPlatform.PUBLIC, JobPlatform.PUBLIC_PERSONNEL,
                JobPlatform.ROCKETPUNCH, JobPlatform.JOBABA);
        List<JobFeedChange> history = new ArrayList<>();
        for (int i = 0; i < platforms.size(); i++) {
            JobFeedChange change = change((long) i + 1, "변경 회사 " + i, NOW.minusHours(1));
            ReflectionTestUtils.setField(change, "platform", platforms.get(i));
            history.add(change);
            when(cards.findJobFeedChangeTargets(change.getPlatform(), change.getExternalId(), change.getChangedAt()))
                    .thenReturn(List.of(card(1L)));
        }
        List<Long> ids = List.of(1L, 2L, 3L, 4L);
        when(changes.findByIdInAndProcessedAtIsNullOrderByChangedAtAscIdAsc(ids)).thenReturn(history);

        var result = service.dispatchChanges(ids);

        assertThat(result.changedPostings()).isEqualTo(4);
        assertThat(result.inAppNotifications()).isEqualTo(4);
        assertThat(result.emailBatches()).isEqualTo(1);
        var message = ArgumentCaptor.forClass(NotificationMailMessage.class);
        verify(mailSender, times(1)).send(eq("user1@example.com"), message.capture());
        assertThat(message.getValue().plainText()).contains("변경 회사 0", "변경 회사 1", "변경 회사 2", "변경 회사 3");
    }

    @Test
    void 현재_수집의_이력만_모아_사용자별_한통씩_발송하고_실제_결과를_집계한다() {
        JobFeedChange first = change(1L, "새 회사", NOW.minusHours(2));
        JobFeedChange second = change(2L, "다음 회사", NOW.minusHours(1));
        List<Long> ids = List.of(1L, 2L);
        when(changes.findByIdInAndProcessedAtIsNullOrderByChangedAtAscIdAsc(ids))
                .thenReturn(List.of(first, second));
        for (JobFeedChange change : List.of(first, second)) {
            when(cards.findJobFeedChangeTargets(change.getPlatform(), change.getExternalId(), change.getChangedAt()))
                    .thenReturn(List.of(card(1L), card(2L)));
        }
        doNothing().when(mailSender).send(eq("user1@example.com"), any());
        doThrow(new IllegalStateException("SMTP 실패")).when(mailSender).send(eq("user2@example.com"), any());

        var result = service.dispatchChanges(ids);

        assertThat(result.changedPostings()).isEqualTo(1);
        assertThat(result.changeHistories()).isEqualTo(2);
        assertThat(result.inAppNotifications()).isEqualTo(4);
        assertThat(result.emailBatches()).isEqualTo(2);
        assertThat(result.emailSuccesses()).isEqualTo(1);
        assertThat(result.emailFailures()).isEqualTo(1);
        verify(mailSender, times(1)).send(eq("user1@example.com"), any());
        verify(mailSender, times(1)).send(eq("user2@example.com"), any());
        verify(changes, never()).findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(any());
    }

    @Test
    void 이미_처리한_수집_이력을_다시_요청하면_발송하지_않는다() {
        when(changes.findByIdInAndProcessedAtIsNullOrderByChangedAtAscIdAsc(List.of(1L)))
                .thenReturn(List.of());

        var result = service.dispatchChanges(List.of(1L));

        assertThat(result.changeHistories()).isZero();
        assertThat(result.inAppNotifications()).isZero();
        assertThat(result.emailBatches()).isZero();
        verifyNoInteractions(mailSender, cards);
    }

    @Test
    void 같은_사용자의_변경을_발생순으로_모아_커밋_후_메일_한통을_발송한다() {
        JobFeedChange first = change(1L, "새 회사", NOW.minusHours(2));
        JobFeedChange second = change(2L, "다음 회사", NOW.minusHours(1));
        KanbanCard card = card(1L);
        targets(List.of(first, second), List.of(card));

        service.dispatch(NOW);

        assertThat(saved).hasSize(4);
        assertThat(saved).allSatisfy(n -> {
            assertThat(n.getReason()).isEqualTo(NotificationReason.JOB_FEED_CHANGED);
            assertThat(n.getDaysBeforeDeadline()).isNull();
            assertThat(n.getStatus()).isEqualTo(NotificationStatus.SUCCESS);
        });
        ArgumentCaptor<NotificationMailMessage> message = ArgumentCaptor.forClass(NotificationMailMessage.class);
        var order = inOrder(transactionManager, mailSender);
        order.verify(transactionManager).getTransaction(any());
        order.verify(transactionManager).commit(any());
        order.verify(mailSender).send(eq("user1@example.com"), message.capture());
        assertThat(message.getValue().plainText()).containsSubsequence("새 회사", "다음 회사");
        assertThat(first.getProcessedAt()).isEqualTo(NOW);
        assertThat(second.getProcessedAt()).isEqualTo(NOW);
        assertThat(card.getApplicationPosting().getCompanyName()).isEqualTo("기존 회사");
    }

    @Test
    void 두_채널을_끄면_발송하지_않고_이력을_처리한다() {
        JobFeedChange change = change(1L, "새 회사", NOW.minusHours(1));
        KanbanCard card = card(1L);
        targets(List.of(change), List.of(card));
        NotificationSetting setting = NotificationSetting.createDefault(card.getUser());
        setting.update(false, false, List.of(1));
        when(settings.findByUser_Id(1L)).thenReturn(Optional.of(setting));

        service.dispatch(NOW);

        assertThat(saved).isEmpty();
        verifyNoInteractions(mailSender);
        assertThat(change.getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    void 이메일을_끄면_인앱만_생성한다() {
        KanbanCard card = card(1L);
        targets(List.of(change(1L, "새 회사", NOW.minusHours(1))), List.of(card));
        NotificationSetting setting = NotificationSetting.createDefault(card.getUser());
        setting.update(false, true, List.of());
        when(settings.findByUser_Id(1L)).thenReturn(Optional.of(setting));

        service.dispatch(NOW);

        assertThat(saved).extracting(Notification::getType).containsExactly(NotificationType.IN_APP);
        verifyNoInteractions(mailSender);
    }

    @Test
    void 여러_사용자는_각자_한통씩_받고_인앱을_끄면_이메일만_받는다() {
        KanbanCard first = card(1L);
        KanbanCard second = card(2L);
        targets(List.of(change(1L, "새 회사", NOW.minusHours(1))), List.of(first, second));
        NotificationSetting setting = NotificationSetting.createDefault(first.getUser());
        setting.update(true, false, List.of());
        when(settings.findByUser_Id(1L)).thenReturn(Optional.of(setting));

        service.dispatch(NOW);

        verify(mailSender).send(eq("user1@example.com"), any());
        verify(mailSender).send(eq("user2@example.com"), any());
        assertThat(saved).hasSize(3);
        assertThat(saved.stream().filter(n -> n.getUser().getId().equals(1L)))
                .extracting(Notification::getType).containsExactly(NotificationType.EMAIL);
    }

    @Test
    void 메일_실패를_기록하고_다른_사용자의_발송은_계속한다() {
        targets(List.of(change(1L, "새 회사", NOW.minusHours(1))), List.of(card(1L), card(2L)));
        doThrow(new IllegalStateException("SMTP 실패")).when(mailSender).send(eq("user1@example.com"), any());

        service.dispatch(NOW);

        assertThat(saved.stream().filter(n -> n.getType() == NotificationType.EMAIL))
                .extracting(Notification::getStatus)
                .containsExactly(NotificationStatus.FAILED, NotificationStatus.SUCCESS);
        assertThat(saved.stream().filter(n -> n.getType() == NotificationType.IN_APP))
                .allSatisfy(n -> assertThat(n.getStatus()).isEqualTo(NotificationStatus.SUCCESS));
    }

    @Test
    void 이미_알린_변경과_카드와_채널은_중복_생성하지_않는다() {
        targets(List.of(change(1L, "새 회사", NOW.minusHours(1))), List.of(card(1L)));
        when(notifications.existsByJobFeedChange_IdAndKanbanCard_IdAndType(eq(1L), eq(1L), any()))
                .thenReturn(true);

        service.dispatch(NOW);

        assertThat(saved).isEmpty();
        verifyNoInteractions(mailSender);
    }

    @Test
    void 처리할_이력이_없으면_재실행해도_발송하지_않는다() {
        when(changes.findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(NOW))
                .thenReturn(List.of());
        service.dispatch(NOW);
        service.dispatch(NOW);
        verifyNoInteractions(cards, settings, mailSender);
    }

    @Test
    void 저장값이_같아도_알리고_연결된_카드가_없으면_알리지_않는다() {
        JobFeedChange first = change(1L, "새 회사", NOW.minusHours(2));
        JobFeedChange second = change(2L, "다음 회사", NOW.minusHours(1));
        KanbanCard card = card(1L);
        card.getApplicationPosting().updateDetails("새 회사", "제목", null, null);
        when(changes.findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(NOW))
                .thenReturn(List.of(first, second));
        when(cards.findJobFeedChangeTargets(first.getPlatform(), first.getExternalId(), first.getChangedAt()))
                .thenReturn(List.of(card));
        when(cards.findJobFeedChangeTargets(second.getPlatform(), second.getExternalId(), second.getChangedAt()))
                .thenReturn(List.of());

        service.dispatch(NOW);

        verify(mailSender).send(eq("user1@example.com"), any());
        assertThat(saved).hasSize(2);
        assertThat(first.getProcessedAt()).isEqualTo(NOW);
        assertThat(second.getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    void 알림_저장이_실패하면_롤백하고_메일을_발송하지_않는다() {
        targets(List.of(change(1L, "새 회사", NOW.minusHours(1))), List.of(card(1L)));
        doThrow(new IllegalStateException("DB 실패")).when(notifications).save(any());

        assertThatThrownBy(() -> service.dispatch(NOW)).isInstanceOf(IllegalStateException.class);
        verify(transactionManager).rollback(any());
        verifyNoInteractions(mailSender);
    }

    @Test
    void 변경값에_HTML이_포함되어도_메일에는_이스케이프한다() {
        targets(List.of(change(1L, "<script>회사</script>", NOW.minusHours(1))), List.of(card(1L)));
        service.dispatch(NOW);
        ArgumentCaptor<NotificationMailMessage> message = ArgumentCaptor.forClass(NotificationMailMessage.class);
        verify(mailSender).send(any(), message.capture());
        assertThat(message.getValue().htmlText()).contains("&lt;script&gt;").doesNotContain("<script>");
    }

    private void targets(List<JobFeedChange> history, List<KanbanCard> targets) {
        when(changes.findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(NOW))
                .thenReturn(history);
        for (JobFeedChange change : history) {
            when(cards.findJobFeedChangeTargets(change.getPlatform(), change.getExternalId(), change.getChangedAt()))
                    .thenReturn(targets);
        }
    }

    private JobFeedChange change(Long id, String newCompany, LocalDateTime changedAt) {
        JobFeed feed = JobFeed.create("source-1", "기존 회사", "제목", null, null, null,
                JobPlatform.PUBLIC, Set.of(), null, null, null);
        JobFeedChange change = JobFeedChange.capture(feed, newCompany, null, null, changedAt);
        ReflectionTestUtils.setField(change, "id", id);
        return change;
    }

    private KanbanCard card(Long userId) {
        User user = User.create("user" + userId + "@example.com", "사용자", null, AuthProvider.KAKAO,
                "provider-" + userId);
        ReflectionTestUtils.setField(user, "id", userId);
        ApplicationPosting posting = ApplicationPosting.createDirect(user, "기존 회사", "제목", null, null);
        KanbanStage stage = KanbanStage.kanbanDefault(user, "지원 완료", 1);
        KanbanCard card = KanbanCard.createCard(stage, posting, 1);
        ReflectionTestUtils.setField(card, "id", userId);
        return card;
    }
}
