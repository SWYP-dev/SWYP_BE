package com.chwihap.server.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {
    @Mock private NotificationDispatchService deadlineService;
    @Mock private JobFeedChangeDispatchService changeService;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock lock;
    @Mock private RLock batchLock;
    @InjectMocks private NotificationScheduler scheduler;

    @BeforeEach
    void setUp() throws InterruptedException {
        ReflectionTestUtils.setField(scheduler, "schedulerZone", "Asia/Seoul");
        when(redissonClient.getLock("lock:deadline-notification")).thenReturn(lock);
        lenient().when(redissonClient.getLock("lock:job-feed-batch-sync")).thenReturn(batchLock);
        lenient().when(batchLock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        lenient().when(batchLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void 통합_수집_중이면_변경_알림을_먼저_발송하지_않는다() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(batchLock.tryLock(0, TimeUnit.SECONDS)).thenReturn(false);

        scheduler.dispatch();

        verifyNoInteractions(changeService);
        verify(deadlineService).dispatch(any(), any());
        verify(lock).unlock();
    }

    @Test
    void 락을_얻지_못하면_두_알림을_모두_건너뛴다() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(false);
        scheduler.dispatch();
        verifyNoInteractions(deadlineService, changeService);
    }

    @Test
    void 마감_알림이_실패해도_변경_알림은_실행하고_락을_해제한다() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalStateException("마감 작업 실패")).when(deadlineService).dispatch(any(), any());

        scheduler.dispatch();

        verify(changeService).dispatch(any());
        verify(lock).unlock();
    }

    @Test
    void 변경_알림이_실패해도_락을_해제한다() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalStateException("변경 작업 실패")).when(changeService).dispatch(any());

        scheduler.dispatch();

        verify(deadlineService).dispatch(any(), any());
        verify(lock).unlock();
    }
}
