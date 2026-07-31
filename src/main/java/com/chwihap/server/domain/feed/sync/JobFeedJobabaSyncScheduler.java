package com.chwihap.server.domain.feed.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 잡아바 채용공고 수집 배치 트리거. 다른 채용정보 수집과 동일하게 하루 2회(08시·20시) 수집한다.
 * <p>
 * 이 API는 정렬·날짜 필터가 보장되지 않아 매번 전체 페이지(최대 {@code sync.max-pages})를
 * 순회한다. 다른 소스보다 페이지 수가 훨씬 많아 한 번 수집에 오래 걸릴 수 있어
 * lease 시간을 더 길게 잡는다.
 * <p>
 * 블루/그린 배포 전환 중 신·구 인스턴스가 잠깐 동시에 떠 있는 구간에 스케줄이 겹치면
 * 동일 배치가 중복 실행될 수 있어, Redisson 분산 락으로 한 인스턴스만 실행되도록 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobFeedJobabaSyncScheduler {

    private static final String LOCK_KEY = "lock:job-feed-jobaba-sync";
    private static final long LEASE_TIME_MINUTES = 30;

    private final JobFeedJobabaSyncService jobFeedJobabaSyncService;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 0 8,20 * * *")
    public void sync() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, LEASE_TIME_MINUTES, TimeUnit.MINUTES);
            if (!acquired) {
                log.info("다른 인스턴스가 이미 잡아바 공고 수집 중이라 이번 실행은 스킵합니다.");
                return;
            }
            log.info("잡아바 공고 수집 스케줄러 시작");
            jobFeedJobabaSyncService.sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("잡아바 공고 수집 락 획득 대기 중 인터럽트가 발생했습니다.", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
