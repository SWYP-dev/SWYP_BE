package com.chwihap.server.domain.feed.sync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * 블루/그린 배포 전환 중 신·구 인스턴스가 동시에 배치를 실행할 수 있는 상황을 재현한다.
 * 서로 다른 {@link RedissonClient} 연결(=서로 다른 인스턴스를 흉내)이 동시에
 * {@link JobFeedSyncScheduler#sync()}를 호출했을 때, 분산 락 덕분에 실제 수집 로직은
 * 정확히 한 번만 실행되어야 한다.
 */
class JobFeedSyncSchedulerLockTest {

    private RedissonClient redissonClientA;
    private RedissonClient redissonClientB;

    @BeforeEach
    void setUp() {
        redissonClientA = createRedissonClient();
        redissonClientB = createRedissonClient();
    }

    @AfterEach
    void tearDown() {
        redissonClientA.shutdown();
        redissonClientB.shutdown();
    }

    @Test
    void 두_인스턴스가_동시에_배치를_실행하면_한쪽만_실제로_수집한다() throws Exception {
        JobFeedSyncService jobFeedSyncServiceA = mock(JobFeedSyncService.class);
        JobFeedSyncService jobFeedSyncServiceB = mock(JobFeedSyncService.class);

        AtomicInteger executionCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            executionCount.incrementAndGet();
            return null;
        }).when(jobFeedSyncServiceA).sync();
        doAnswer(invocation -> {
            executionCount.incrementAndGet();
            return null;
        }).when(jobFeedSyncServiceB).sync();

        JobFeedSyncScheduler schedulerA = new JobFeedSyncScheduler(jobFeedSyncServiceA, redissonClientA);
        JobFeedSyncScheduler schedulerB = new JobFeedSyncScheduler(jobFeedSyncServiceB, redissonClientB);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        Callable<Void> triggerA = () -> {
            startSignal.await();
            schedulerA.sync();
            return null;
        };
        Callable<Void> triggerB = () -> {
            startSignal.await();
            schedulerB.sync();
            return null;
        };

        Future<Void> futureA = executor.submit(triggerA);
        Future<Void> futureB = executor.submit(triggerB);

        startSignal.countDown();
        futureA.get(10, TimeUnit.SECONDS);
        futureB.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 어느 인스턴스가 락을 먼저 잡을지는 비결정적이므로, "정확히 하나만 실행됐는지"만 검증한다.
        assertEquals(1, executionCount.get(), "분산 락으로 두 인스턴스 중 정확히 하나만 실제 수집을 실행해야 한다");
    }

    private RedissonClient createRedissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        return Redisson.create(config);
    }
}
