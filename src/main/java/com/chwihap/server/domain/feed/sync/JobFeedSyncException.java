package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.dto.JobFeedSyncResult;
import lombok.Getter;

/** 수집이 중단되어도 앞선 페이지에서 커밋한 변경 이력과 집계는 통합 배치에 전달한다. */
@Getter
public class JobFeedSyncException extends RuntimeException {
    private final JobFeedSyncResult partialResult;

    public JobFeedSyncException(JobFeedSyncResult partialResult, RuntimeException cause) {
        super("공고 수집 중단: " + partialResult.platform(), cause);
        this.partialResult = partialResult;
    }
}
