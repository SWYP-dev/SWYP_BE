package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.dto.JobFeedSyncResult;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.service.JobFeedSyncCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 네 플랫폼을 모두 수집한 뒤 사용자별 통합 변경 메일을 한 번 발송한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobFeedBatchSyncService {
    public static final String LOCK_KEY = "lock:job-feed-batch-sync";
    private final JobFeedSyncScheduler publicScheduler;
    private final JobFeedPersonnelJobSyncScheduler personnelScheduler;
    private final JobFeedRocketPunchSyncScheduler rocketPunchScheduler;
    private final JobFeedJobabaSyncScheduler jobabaScheduler;
    private final JobFeedSyncCompletionService completionService;
    private final RedissonClient redissonClient;

    /** 서버 시간 기준 08시·20시에 실행한다. 긴 수집 중에는 watchdog으로 통합 락을 갱신한다. */
    @Scheduled(cron = "0 0 8,20 * * *")
    public void syncAll() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, TimeUnit.SECONDS);
            if (!acquired) {
                log.info("다른 인스턴스가 통합 공고 수집 중이라 이번 실행을 건너뜁니다.");
                return;
            }
            List<JobFeedSyncResult> results = new ArrayList<>();
            int failures = 0;
            failures += collect(JobPlatform.PUBLIC, publicScheduler::sync, results);
            failures += collect(JobPlatform.PUBLIC_PERSONNEL, personnelScheduler::sync, results);
            failures += collect(JobPlatform.ROCKETPUNCH, rocketPunchScheduler::sync, results);
            failures += collect(JobPlatform.JOBABA, jobabaScheduler::sync, results);
            completionService.completeAll(results, failures);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("통합 공고 수집 락 획득 중 인터럽트가 발생했습니다.", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private int collect(JobPlatform platform, Supplier<JobFeedSyncResult> collect,
                        List<JobFeedSyncResult> results) {
        try {
            results.add(collect.get());
            return 0;
        } catch (JobFeedSyncException e) {
            results.add(e.getPartialResult());
            log.error("플랫폼 수집 중단. 커밋된 페이지는 통합 알림에 포함합니다. platform={}", platform, e);
            return 1;
        } catch (RuntimeException e) {
            log.error("플랫폼 수집 실패. 나머지 플랫폼 수집을 계속합니다. platform={}", platform, e);
            return 1;
        }
    }
}
