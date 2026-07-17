package com.chwihap.server.domain.feed.controller;

import com.chwihap.server.domain.feed.sync.JobFeedSyncService;
import com.chwihap.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 개발 환경에서 cron(매일 08/20시) 대기 없이 공고 수집 배치를 즉시 실행하기 위한 전용 엔드포인트.
 * {@code local} 프로필에서만 등록되며, 운영 환경에는 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/v1/dev/feed-sync")
@RequiredArgsConstructor
@Profile("local")
public class FeedSyncDevController {

    private final JobFeedSyncService jobFeedSyncService;

    @PostMapping
    public ApiResponse<Void> triggerSync() {
        jobFeedSyncService.sync();
        return ApiResponse.success();
    }
}
