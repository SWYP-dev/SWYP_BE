package com.chwihap.server.domain.feed.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.dto.JobFeedSyncResult;
import com.chwihap.server.domain.notification.dto.JobFeedChangeDispatchResult;
import com.chwihap.server.domain.notification.service.JobFeedChangeDispatchService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobFeedSyncCompletionServiceTest {
    @Test
    void 모든_페이지의_이력을_한번에_넘기고_최종_집계를_한줄로_남긴다() {
        var dispatch = mock(JobFeedChangeDispatchService.class);
        List<Long> ids = List.of(10L, 11L, 12L);
        when(dispatch.dispatchChanges(ids)).thenReturn(new JobFeedChangeDispatchResult(2, 3, 5, 2, 1, 1));
        Logger logger = (Logger) LoggerFactory.getLogger(JobFeedSyncCompletionService.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            new JobFeedSyncCompletionService(dispatch).completeAll(List.of(
                    new JobFeedSyncResult(JobPlatform.PUBLIC, 1, 100, 0, List.of(10L)),
                    new JobFeedSyncResult(JobPlatform.PUBLIC_PERSONNEL, 1, 200, 0, List.of(11L)),
                    new JobFeedSyncResult(JobPlatform.ROCKETPUNCH, 1, 150, 0, List.of(12L)),
                    new JobFeedSyncResult(JobPlatform.JOBABA, 1, 200, 0, List.of())), 0);

            verify(dispatch, times(1)).dispatchChanges(ids);
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage()).contains(
                    "changedPostings=2", "changeHistories=3",
                    "inAppNotifications=5", "emailBatches=2", "emailSuccesses=1", "emailFailures=1");
            assertThat(appender.list.get(0).getFormattedMessage())
                    .doesNotContain("created=", "updated=", "skippedPages=");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void 변경이_없는_수집은_알림_발송을_호출하지_않는다() {
        var dispatch = mock(JobFeedChangeDispatchService.class);

        new JobFeedSyncCompletionService(dispatch).completeAll(List.of(
                new JobFeedSyncResult(JobPlatform.PUBLIC_PERSONNEL, 0, 650, 0, List.of())), 0);

        verifyNoInteractions(dispatch);
    }
}
