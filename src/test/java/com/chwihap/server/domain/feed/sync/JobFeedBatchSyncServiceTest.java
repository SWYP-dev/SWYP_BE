package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.dto.JobFeedSyncResult;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.service.JobFeedSyncCompletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobFeedBatchSyncServiceTest {
    @Mock private JobFeedSyncScheduler publicScheduler;
    @Mock private JobFeedPersonnelJobSyncScheduler personnelScheduler;
    @Mock private JobFeedRocketPunchSyncScheduler rocketPunchScheduler;
    @Mock private JobFeedJobabaSyncScheduler jobabaScheduler;
    @Mock private JobFeedSyncCompletionService completion;
    @Mock private RedissonClient redisson;
    @Mock private RLock lock;
    private JobFeedBatchSyncService service;

    @BeforeEach
    void setUp() {
        service = new JobFeedBatchSyncService(publicScheduler, personnelScheduler, rocketPunchScheduler,
                jobabaScheduler, completion, redisson);
        when(redisson.getLock(JobFeedBatchSyncService.LOCK_KEY)).thenReturn(lock);
    }

    @Test
    void 네_플랫폼_수집이_끝난_뒤_한번만_통합_발송한다() throws InterruptedException {
        acquired();
        var first = result(JobPlatform.PUBLIC, 1L);
        var second = result(JobPlatform.PUBLIC_PERSONNEL, 2L);
        var third = result(JobPlatform.ROCKETPUNCH, 3L);
        var fourth = result(JobPlatform.JOBABA, 4L);
        when(publicScheduler.sync()).thenReturn(first);
        when(personnelScheduler.sync()).thenReturn(second);
        when(rocketPunchScheduler.sync()).thenReturn(third);
        when(jobabaScheduler.sync()).thenReturn(fourth);

        service.syncAll();

        var order = inOrder(publicScheduler, personnelScheduler, rocketPunchScheduler, jobabaScheduler, completion, lock);
        order.verify(publicScheduler).sync();
        order.verify(personnelScheduler).sync();
        order.verify(rocketPunchScheduler).sync();
        order.verify(jobabaScheduler).sync();
        order.verify(completion).completeAll(List.of(first, second, third, fourth), 0);
        order.verify(lock).unlock();
        verifyNoMoreInteractions(completion);
    }

    @Test
    void 일부_수집이_실패해도_커밋된_이력과_나머지_플랫폼을_합쳐_발송한다() throws InterruptedException {
        acquired();
        var first = result(JobPlatform.PUBLIC, 1L);
        var partial = result(JobPlatform.PUBLIC_PERSONNEL, 2L);
        var third = result(JobPlatform.ROCKETPUNCH, 3L);
        var fourth = result(JobPlatform.JOBABA, 4L);
        when(publicScheduler.sync()).thenReturn(first);
        when(personnelScheduler.sync()).thenThrow(new JobFeedSyncException(partial, new IllegalStateException("DB 실패")));
        when(rocketPunchScheduler.sync()).thenReturn(third);
        when(jobabaScheduler.sync()).thenReturn(fourth);

        service.syncAll();

        verify(completion).completeAll(List.of(first, partial, third, fourth), 1);
        verify(lock).unlock();
    }

    @Test
    void 다른_서버가_수집중이면_중복_수집과_발송을_하지_않는다() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(false);

        service.syncAll();

        verifyNoInteractions(publicScheduler, personnelScheduler, rocketPunchScheduler, jobabaScheduler, completion);
        verify(lock, never()).unlock();
    }

    private void acquired() throws InterruptedException {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    private JobFeedSyncResult result(JobPlatform platform, Long id) {
        return new JobFeedSyncResult(platform, 0, 1, 0, List.of(id));
    }
}
