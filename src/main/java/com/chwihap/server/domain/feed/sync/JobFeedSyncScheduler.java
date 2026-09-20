package com.chwihap.server.domain.feed.sync;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 채용공고 수집 배치 트리거. 채용공고는 실시간 갱신이 불필요하여 하루 2회(08시·20시) 수집한다.
 * <p>
 * 블루/그린 배포 전환 중 신·구 인스턴스가 잠깐 동시에 떠 있는 구간에 스케줄이 겹치면
 * 동일 배치가 중복 실행될 수 있어, Redisson 분산 락으로 한 인스턴스만 실행되도록 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobFeedSyncScheduler {

    private static final String LOCK_KEY = "lock:job-feed-sync";
    private static final long LEASE_TIME_MINUTES = 10;
    private static final String JOB_TAG = "worknet";

    private final JobFeedSyncService jobFeedSyncService;
    private final RedissonClient redissonClient;
    private final MeterRegistry meterRegistry;

    private final AtomicLong lastSuccessTimestamp = new AtomicLong(0);
    private final AtomicLong lastDurationSeconds = new AtomicLong(0);

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("job_sync_last_success_timestamp_seconds", lastSuccessTimestamp, AtomicLong::get)
                .description("배치 마지막 성공 시각 (epoch seconds)")
                .tag("job", JOB_TAG)
                .register(meterRegistry);
        Gauge.builder("job_sync_last_duration_seconds", lastDurationSeconds, AtomicLong::get)
                .description("배치 마지막 실행 소요 시간(초)")
                .tag("job", JOB_TAG)
                .register(meterRegistry);
    }

    @Scheduled(cron = "0 0 8,20 * * *")
    public void sync() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, LEASE_TIME_MINUTES, TimeUnit.MINUTES);
            if (!acquired) {
                log.info("다른 인스턴스가 이미 공고 수집 중이라 이번 실행은 스킵합니다.");
                return;
            }
            log.info("공고 수집 스케줄러 시작");
            long startMillis = System.currentTimeMillis();
            jobFeedSyncService.sync();
            lastDurationSeconds.set((System.currentTimeMillis() - startMillis) / 1000);
            lastSuccessTimestamp.set(Instant.now().getEpochSecond());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("공고 수집 락 획득 대기 중 인터럽트가 발생했습니다.", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
