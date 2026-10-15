package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.feed.repository.JobFeedChangeRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JobFeedChangeDispatchService {
    private static final DateTimeFormatter CHANGE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JobFeedChangeRepository changeRepository;
    private final KanbanCardRepository cardRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;
    private final NotificationMailSender mailSender;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.notification.scheduler-zone:Asia/Seoul}")
    private String schedulerZone = "Asia/Seoul";

    public JobFeedChangeDispatchService(JobFeedChangeRepository changeRepository,
            KanbanCardRepository cardRepository, NotificationRepository notificationRepository,
            NotificationSettingRepository settingRepository, NotificationMailSender mailSender,
            PlatformTransactionManager transactionManager) {
        this.changeRepository = changeRepository;
        this.cardRepository = cardRepository;
        this.notificationRepository = notificationRepository;
        this.settingRepository = settingRepository;
        this.mailSender = mailSender;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // 외부 트랜잭션에서 호출해도 메일 발송 전에 알림 저장을 커밋한다.
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void dispatch(LocalDateTime cutoff) {
        List<MailBatch> batches = transactionTemplate.execute(status -> prepare(cutoff));
        if (batches == null) {
            return;
        }
        for (MailBatch batch : batches) {
            LocalDateTime attemptedAt = LocalDateTime.now(ZoneId.of(schedulerZone));
            NotificationStatus result;
            try {
                mailSender.send(batch.recipient(), batch.message());
                result = NotificationStatus.SUCCESS;
            } catch (RuntimeException e) {
                result = NotificationStatus.FAILED;
                log.warn("공고 변경 요약 이메일 발송 실패. userId={}", batch.userId(), e);
            }
            NotificationStatus finalResult = result;
            transactionTemplate.executeWithoutResult(status ->
                    notificationRepository.findAllById(batch.notificationIds()).forEach(notification ->
                            notification.completeEmail(finalResult, attemptedAt)));
        }
    }

    private List<MailBatch> prepare(LocalDateTime cutoff) {
        List<JobFeedChange> changes = changeRepository
                .findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(cutoff);
        Map<Long, List<Notification>> emails = new LinkedHashMap<>();
        Map<Long, NotificationSetting> settings = new LinkedHashMap<>();
        for (JobFeedChange change : changes) {
            List<KanbanCard> cards = cardRepository.findJobFeedChangeTargets(
                    change.getPlatform(), change.getExternalId(), change.getChangedAt());
            for (KanbanCard card : cards) {
                List<String> differences = change.describeDifferences(card.getApplicationPosting());
                if (differences.isEmpty()) {
                    continue;
                }
                Long userId = card.getUser().getId();
                if (!settings.containsKey(userId)) {
                    settings.put(userId, settingRepository.findByUser_Id(userId).orElse(null));
                }
                NotificationSetting setting = settings.get(userId);
                String message = change.getChangedAt().format(CHANGE_TIME_FORMAT) + " · "
                        + card.getApplicationPosting().getCompanyName() + " · "
                        + card.getApplicationPosting().getTitle() + "\n" + String.join("\n", differences);
                if (setting == null || setting.isInAppEnabled()) {
                    createNotification(change, card, NotificationType.IN_APP, message);
                }
                if (setting == null || setting.isEmailEnabled()) {
                    Notification email = createNotification(change, card, NotificationType.EMAIL, message);
                    if (email != null) {
                        emails.computeIfAbsent(userId, id -> new ArrayList<>()).add(email);
                    }
                }
            }
            change.markProcessed(cutoff);
        }
        notificationRepository.flush();
        return emails.entrySet().stream().map(entry -> {
            List<Notification> notifications = entry.getValue();
            String text = "저장한 공고의 원본 정보가 변경되었습니다.\n\n"
                    + notifications.stream().map(Notification::getMessage).collect(Collectors.joining("\n\n"))
                    + "\n\n칸반에 저장한 정보는 유지됩니다. 이메일 수신 여부는 알림 설정에서 변경할 수 있습니다.";
            NotificationMailMessage message = new NotificationMailMessage(
                    "[취합] 공고 정보 변경 " + notifications.size() + "건 안내", text,
                    "<html lang=\"ko\"><body><p>" + HtmlUtils.htmlEscape(text).replace("\n", "<br>")
                            + "</p></body></html>");
            return new MailBatch(entry.getKey(), notifications.get(0).getUser().getEmail(), message,
                    notifications.stream().map(Notification::getId).toList());
        }).toList();
    }

    private Notification createNotification(JobFeedChange change, KanbanCard card,
                                             NotificationType channel, String message) {
        if (notificationRepository.existsByJobFeedChange_IdAndKanbanCard_IdAndType(
                change.getId(), card.getId(), channel)) {
            return null;
        }
        return notificationRepository.save(Notification.jobFeedChanged(
                card.getUser(), card, change, channel, message));
    }

    private record MailBatch(Long userId, String recipient, NotificationMailMessage message,
                             List<Long> notificationIds) {
    }
}
