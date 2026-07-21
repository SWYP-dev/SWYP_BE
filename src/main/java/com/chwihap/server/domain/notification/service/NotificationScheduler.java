package com.chwihap.server.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final String LOCK_KEY = "lock:deadline-notification";
    private static final long LEASE_TIME_MINUTES = 30;

    private final NotificationDispatchService notificationDispatchService;
    private final RedissonClient redissonClient;

    @Value("${app.notification.scheduler-zone:Asia/Seoul}")
    private String schedulerZone;

    /**
     * 마감일 알림 스케줄러<br>
     * 매일 오전 9시에 분산 락을 획득한 인스턴스에서 마감일 알림 발송을 실행한다.
     */
    @Scheduled(
            cron = "0 0 9 * * *",
            zone = "${app.notification.scheduler-zone:Asia/Seoul}"
    )
    public void dispatch() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, LEASE_TIME_MINUTES, TimeUnit.MINUTES);
            if (!acquired) {
                log.info("다른 인스턴스가 이미 마감 알림을 발송 중이라 이번 실행은 스킵합니다.");
                return;
            }

            log.info("알림 스케줄러 시작");
            ZoneId zoneId = ZoneId.of(schedulerZone);
            notificationDispatchService.dispatch(LocalDate.now(zoneId), LocalDateTime.now(zoneId));
            log.info("알림 스케줄러 완료");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("마감 알림 락 획득 대기 중 인터럽트가 발생했습니다.", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
