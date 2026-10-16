package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.feed.repository.JobFeedChangeRepository;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.notification.entity.Notification;
import com.chwihap.server.domain.notification.dto.JobFeedChangeDispatchResult;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 원본 공고 변경 이력을 바탕으로 칸반 카드 사용자의 인앱 알림과 요약 이메일을 처리한다.
 * <p>전체 수집 완료 후 해당 실행에서 커밋한 변경 이력을 모아 호출하며,
 * 기존 {@link NotificationScheduler}는 미처리 이력을 보완 처리한다.
 * 알림 저장을 커밋한 뒤 이메일을 발송한다.
 * 사용자가 칸반에 저장한 공고 정보는 변경하지 않는다.
 */
@Slf4j
@Service
public class JobFeedChangeDispatchService {
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

    /**
     * 기준 시각까지의 공고 변경 알림을 준비하고 사용자별 요약 이메일을 발송한다.
     * <p>알림 생성과 변경 이력 처리 완료 기록을 별도 트랜잭션에서 커밋한 뒤,
     * 해당 트랜잭션 밖에서 이메일을 발송한다. 발송 성공 또는 실패 결과는
     * 별도 트랜잭션으로 기록하며, 한 사용자의 발송 실패가 다음 사용자 발송을 막지 않는다.
     * 실패한 이메일은 이 메서드에서 자동 재시도하지 않는다.
     *
     * @param cutoff 처리할 변경 이력의 발생 시각 상한이자 처리 완료 기록 시각
     * @return 실제 처리한 변경 이력·인앱 알림 수와 이메일 발송 결과
     */
    public JobFeedChangeDispatchResult dispatch(LocalDateTime cutoff) {
        return dispatch(() -> changeRepository
                .findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(cutoff), cutoff);
    }

    /** 현재 수집 실행에서 커밋된 변경 이력만 모아 사용자별 이메일 한 통으로 발송한다. */
    public JobFeedChangeDispatchResult dispatchChanges(List<Long> changeIds) {
        if (changeIds.isEmpty()) {
            return JobFeedChangeDispatchResult.empty();
        }
        return dispatch(() -> changeRepository.findByIdInAndProcessedAtIsNullOrderByChangedAtAscIdAsc(changeIds),
                LocalDateTime.now(ZoneId.of(schedulerZone)));
    }

    private JobFeedChangeDispatchResult dispatch(Supplier<List<JobFeedChange>> loadChanges, LocalDateTime cutoff) {
        PreparedBatch prepared = transactionTemplate.execute(status -> prepare(loadChanges.get(), cutoff));
        if (prepared == null) {
            return JobFeedChangeDispatchResult.empty();
        }
        int successes = 0;
        int failures = 0;
        for (MailBatch batch : prepared.emails()) {
            LocalDateTime attemptedAt = LocalDateTime.now(ZoneId.of(schedulerZone));
            NotificationStatus result;
            try {
                mailSender.send(batch.recipient(), batch.message());
                result = NotificationStatus.SUCCESS;
                successes++;
            } catch (RuntimeException e) {
                result = NotificationStatus.FAILED;
                failures++;
                log.warn("공고 변경 요약 이메일 발송 실패. userId={}", batch.userId(), e);
            }
            NotificationStatus finalResult = result;
            transactionTemplate.executeWithoutResult(status ->
                    notificationRepository.findAllById(batch.notificationIds()).forEach(notification ->
                            notification.completeEmail(finalResult, attemptedAt)));
        }
        return new JobFeedChangeDispatchResult(prepared.changedPostings(), prepared.changeHistories(),
                prepared.inAppNotifications(), prepared.emails().size(), successes, failures);
    }

    /**
     * 미처리 변경 이력을 조회해 알림을 저장하고 사용자별 이메일 발송 데이터를 준비한다.
     * <p>호출자가 연 트랜잭션 안에서 실행하며 실제 이메일은 발송하지 않는다.
     * 변경 전에 생성되어 해당 원본 공고와 연결된 칸반 카드를 찾고,
     * 사용자 저장값과 관계없이 원본에서 변경된 항목을 알림에 포함한다.
     * 사용자 설정이 없으면 두 채널 모두 허용하며, 설정이 있으면 활성화된 채널만 생성한다.
     * 대상 카드나 생성할 알림이 없는 경우에도 해당 변경 이력은 처리 완료로 기록한다.
     *
     * @param changes 잠금을 획득한 미처리 변경 이력
     * @param cutoff 처리 완료 기록 시각
     * @return 사용자별 이메일 발송 데이터와 알림 생성 집계
     */
    private PreparedBatch prepare(List<JobFeedChange> changes, LocalDateTime cutoff) {
        Map<Long, List<Notification>> emails = new LinkedHashMap<>();
        Map<Long, NotificationSetting> settings = new LinkedHashMap<>();
        int inAppCount = 0;
        for (JobFeedChange change : changes) {
            List<KanbanCard> cards = cardRepository.findJobFeedChangeTargets(
                    change.getPlatform(), change.getExternalId(), change.getChangedAt());
            List<String> differences = change.describeDifferences();
            for (KanbanCard card : cards) {
                // 만약 공고가 바뀐 값이 없을 때(리스트가 비었을 때)
                if (differences.isEmpty()) {
                    continue;   // 남은 코드 건너뛰고 반복으로 이동
                }
                Long userId = card.getUser().getId();
                // [설정] 아직 조회하지 않은 사용자의 알림 수신 설정을 조회해 Map에 저장한다.
                if (!settings.containsKey(userId)) {
                    settings.put(userId, settingRepository.findByUser_Id(userId).orElse(null));
                }
                NotificationSetting setting = settings.get(userId);
                String message = card.getApplicationPosting().getCompanyName() + " · "
                        + card.getApplicationPosting().getTitle() + "\n" + String.join("\n", differences);
                // 설정이 없거나 인앱 알림을 허용한 사용자의 알림을 생성한다.
                if (setting == null || setting.isInAppEnabled()) {
                    if (createNotification(change, card, NotificationType.IN_APP, message) != null) {
                        inAppCount++;
                    }
                }
                // 설정이 없거나 이메일을 허용한 사용자를 발송 목록에 추가한다.
                if (setting == null || setting.isEmailEnabled()) {
                    Notification email = createNotification(change, card, NotificationType.EMAIL, message);
                    if (email != null) {
                        emails.computeIfAbsent(userId, id -> new ArrayList<>()).add(email);
                    }
                }
            }
            change.markProcessed(cutoff);
        }
        // 영속성 컨텍스트의 변경 사항을 DB에 반영한다.
        notificationRepository.flush();

        // 동일한 원본 공고의 여러 변경 이력은 공고 수에서 중복 집계하지 않는다.
        long postingCount = changes.stream()
                .map(change -> Map.entry(
                        change.getPlatform(), change.getExternalId()))
                .distinct()
                .count();
        List<MailBatch> batches = emails.entrySet().stream().map(entry -> {
            List<Notification> notifications = entry.getValue();
            NotificationMailMessage message = NotificationMailMessage.jobFeedChanges(notifications);
            return new MailBatch(entry.getKey(), notifications.get(0).getUser().getEmail(), message,
                    notifications.stream().map(Notification::getId).toList());
        }).toList();
        return new PreparedBatch(batches, postingCount, changes.size(), inAppCount);
    }

    private record PreparedBatch(List<MailBatch> emails, long changedPostings,
                                 int changeHistories, int inAppNotifications) {
    }

    /**
     * 동일한 변경 이력·칸반 카드·채널 조합의 알림이 없을 때 알림을 저장한다.
     * <p>인앱 알림은 성공 상태로, 이메일 알림은 발송 대기 상태로 생성한다.
     * 실제 이메일 발송은 {@link #dispatch(LocalDateTime)}에서 수행한다.
     *
     * @param change 원본 공고 변경 이력
     * @param card 알림 대상 사용자의 칸반 카드
     * @param channel 알림 채널
     * @param message 변경 내용을 설명하는 알림 본문
     * @return 저장한 알림, 동일 조합의 알림이 이미 있으면 {@code null}
     */
    private Notification createNotification(
            JobFeedChange change,
            KanbanCard card,
            NotificationType channel,
            String message
    ) {
        if (notificationRepository.existsByJobFeedChange_IdAndKanbanCard_IdAndType(
                change.getId(),
                card.getId(),
                channel)
        ) {
            return null;
        }
        return notificationRepository.save(
                Notification.jobFeedChanged(
                card.getUser(),
                        card,
                        change,
                        channel,
                        message
                )
        );
    }

    /**
     * 한 사용자에게 보낼 요약 이메일과 발송 결과를 갱신할 알림 ID를 담는다.
     *
     * @param userId 수신 사용자 ID
     * @param recipient 수신 이메일 주소
     * @param message 이메일 제목과 일반 텍스트·HTML 본문
     * @param notificationIds 발송 결과를 함께 기록할 이메일 알림 ID 목록
     */
    private record MailBatch(
            Long userId,
            String recipient,
            NotificationMailMessage message,
            List<Long> notificationIds
    ) {
    }
}
