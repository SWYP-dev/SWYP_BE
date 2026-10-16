package com.chwihap.server.domain.feed.dto;

import com.chwihap.server.domain.feed.enums.JobPlatform;
import java.util.List;

/** 플랫폼 수집 결과. 변경 이력 ID는 페이지 트랜잭션이 커밋된 뒤에만 포함한다. */
public record JobFeedSyncResult(JobPlatform platform, int created, int updated,
                                int skippedPages, List<Long> changeIds) {
    public JobFeedSyncResult {
        changeIds = List.copyOf(changeIds);
    }

    public static JobFeedSyncResult empty(JobPlatform platform) {
        return new JobFeedSyncResult(platform, 0, 0, 0, List.of());
    }
}
