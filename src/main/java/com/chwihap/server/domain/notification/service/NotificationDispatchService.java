package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.notification.entity.Notification;
import com.chwihap.server.domain.notification.entity.NotificationSetting;
import com.chwihap.server.domain.notification.enums.NotificationStatus;
import com.chwihap.server.domain.notification.enums.NotificationType;
import com.chwihap.server.domain.notification.mail.NotificationMailMessage;
import com.chwihap.server.domain.notification.mail.NotificationMailSender;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private static final List<Integer> DEFAULT_REMIND_DAYS = List.of(7, 3, 1);
    private static final Set<Integer> SUPPORTED_REMIND_DAYS = Set.copyOf(DEFAULT_REMIND_DAYS);

    private final KanbanCardRepository kanbanCardRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMailSender notificationMailSender;

    /**
     * 마감일 알림 발송<br>
     * 오늘을 기준으로 D-7/D-3/D-1 카드의 인앱 알림을 발송하고,
     * 이메일은 유저별로 마감 임박 카드를 모아 다이제스트 메일 한 통으로 발송한다.
     * @param today 알림 기준 날짜
     * @param now 알림 발송 시각
     * @author say_0
     */
    @Transactional
    public void dispatch(LocalDate today, LocalDateTime now) {
        List<LocalDate> targetDeadlines = DEFAULT_REMIND_DAYS.stream()
                .map(today::plusDays)
                .toList();
        List<KanbanCard> cards = kanbanCardRepository.findDeadlineReminderTargets(targetDeadlines);
        if (cards.isEmpty()) {
            return;
        }

        Set<Long> userIds = cards.stream()
                .map(card -> card.getUser().getId())
                .collect(Collectors.toSet());
        Map<Long, NotificationSetting> settings = notificationSettingRepository.findByUser_IdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        setting -> setting.getUser().getId(),
                        Function.identity()
                ));

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1).atStartOfDay();

        Map<Long, List<KanbanCard>> emailTargetsByUser = new LinkedHashMap<>();

        for (KanbanCard card : cards) {
            int daysLeft = (int) ChronoUnit.DAYS.between(today, card.getJobPosting().getDeadline());
            NotificationSetting setting = settings.get(card.getUser().getId());
            List<Integer> remindDays = setting == null ? DEFAULT_REMIND_DAYS : setting.getRemindDays();
            if (!SUPPORTED_REMIND_DAYS.contains(daysLeft) || !remindDays.contains(daysLeft)) {
                continue;
            }

            boolean emailEnabled = setting == null || setting.isEmailEnabled();
            boolean inAppEnabled = setting == null || setting.isInAppEnabled();

            // 이메일 발송 대상을 유저별로 모으는 로직
            if (emailEnabled && !wasCreatedToday(card, NotificationType.EMAIL, dayStart, nextDayStart)) {
                emailTargetsByUser
                        .computeIfAbsent(card.getUser().getId(), id -> new ArrayList<>())
                        .add(card);
            }

            // 인앱 알림을 저장하는 로직
            if (inAppEnabled && !wasCreatedToday(card, NotificationType.IN_APP, dayStart, nextDayStart)) {
                String message = card.getJobPosting().getCompanyName() + " 지원 마감 D-" + daysLeft + "입니다.";
                notificationRepository.save(Notification.inApp(card.getUser(), card, message));
            }
        }

        for (List<KanbanCard> userCards : emailTargetsByUser.values()) {
            sendDigestEmail(userCards, today, now);
        }
    }

    private void sendDigestEmail(List<KanbanCard> cards, LocalDate today, LocalDateTime now) {
        NotificationStatus status;
        try {
            NotificationMailMessage mailMessage = NotificationMailMessage.deadlineDigest(cards, today);
            notificationMailSender.send(cards.get(0).getUser().getEmail(), mailMessage);
            status = NotificationStatus.SUCCESS;
        } catch (RuntimeException e) {
            status = NotificationStatus.FAILED;
            log.warn("마감 알림 다이제스트 이메일 발송에 실패했습니다. userId={}",
                    cards.get(0).getUser().getId(), e);
        }

        for (KanbanCard card : cards) {
            int daysLeft = (int) ChronoUnit.DAYS.between(today, card.getJobPosting().getDeadline());
            String message = card.getJobPosting().getCompanyName() + " 지원 마감 D-" + daysLeft + "입니다.";
            notificationRepository.save(Notification.email(card.getUser(), card, message, status, now));
        }
    }

    private boolean wasCreatedToday(
            KanbanCard card,
            NotificationType type,
            LocalDateTime dayStart,
            LocalDateTime nextDayStart
    ) {
        return notificationRepository
                .existsByUser_IdAndKanbanCard_IdAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        card.getUser().getId(), card.getId(), type, dayStart, nextDayStart);
    }
}
