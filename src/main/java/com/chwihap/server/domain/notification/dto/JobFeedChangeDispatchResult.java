package com.chwihap.server.domain.notification.dto;

/** 알림 준비 트랜잭션과 이메일 발송이 끝난 뒤 집계하는 처리 결과. 이메일은 사용자별 통수다. */
public record JobFeedChangeDispatchResult(
        long changedPostings,
        int changeHistories,
        int inAppNotifications,
        int emailBatches,
        int emailSuccesses,
        int emailFailures
) {
    public static JobFeedChangeDispatchResult empty() {
        return new JobFeedChangeDispatchResult(0, 0, 0, 0, 0, 0);
    }
}
