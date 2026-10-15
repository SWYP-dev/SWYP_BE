package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.feed.dto.JobFeedSyncResult;
import com.chwihap.server.domain.notification.dto.JobFeedChangeDispatchResult;
import com.chwihap.server.domain.notification.service.JobFeedChangeDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/** 수집 전체가 끝나면 커밋된 변경 이력을 한 번에 발송하고 최종 결과를 기록한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobFeedSyncCompletionService {
    private final JobFeedChangeDispatchService dispatchService;

    /** 네 플랫폼의 변경 이력 ID를 합쳐 사용자별 통합 메일을 한 번만 준비한다. */
    public void completeAll(List<JobFeedSyncResult> results, int failedPlatforms) {
        List<Long> ids = results.stream().flatMap(result -> result.changeIds().stream()).distinct().toList();
        complete(ids, failedPlatforms);
    }

    private void complete(List<Long> changeIds, int failedPlatforms) {
        JobFeedChangeDispatchResult result;
        try {
            result = changeIds.isEmpty() ? JobFeedChangeDispatchResult.empty()
                    : dispatchService.dispatchChanges(changeIds);
        } catch (RuntimeException e) {
            log.error("통합 변경 알림 처리 실패: changeHistories={}, failedPlatforms={}",
                    changeIds.size(), failedPlatforms, e);
            return;
        }
        log.info("통합 변경 알림 처리 완료: changedPostings={}, changeHistories={}, inAppNotifications={}, "
                        + "emailBatches={}, emailSuccesses={}, emailFailures={}, failedPlatforms={}",
                result.changedPostings(), result.changeHistories(),
                result.inAppNotifications(), result.emailBatches(), result.emailSuccesses(),
                result.emailFailures(), failedPlatforms);
    }
}
