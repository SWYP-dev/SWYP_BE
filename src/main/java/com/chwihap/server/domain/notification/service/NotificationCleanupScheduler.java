package com.chwihap.server.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private static final String LOCK_KEY = "lock:notification-cleanup";
    private static final long LEASE_TIME_MINUTES = 30;

    private final NotificationCleanupService notificationCleanupService;
    private final RedissonClient redissonClient;

    @Value("${app.notification.scheduler-zone:Asia/Seoul}")
    private String schedulerZone;

    @Value("${app.notification.retention-days:90}")
    private int retentionDays;

    @Scheduled(
            cron = "0 * * * * *",
            zone = "${app.notification.scheduler-zone:Asia/Seoul}"
    )
    public void cleanup() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, LEASE_TIME_MINUTES, TimeUnit.MINUTES);
            if (!acquired) {
                log.info("다른 인스턴스가 알림 이력을 정리 중이라 이번 실행은 스킵합니다.");
                return;
            }

            ZoneId zoneId = ZoneId.of(schedulerZone);
            int deletedCount = notificationCleanupService.deleteExpiredNotifications(
                    LocalDateTime.now(zoneId),
                    retentionDays
            );
            log.info("만료된 알림 이력 정리 완료. retentionDays={}, deletedCount={}",
                    retentionDays, deletedCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("알림 이력 정리 락 획득 중 인터럽트가 발생했습니다.", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
