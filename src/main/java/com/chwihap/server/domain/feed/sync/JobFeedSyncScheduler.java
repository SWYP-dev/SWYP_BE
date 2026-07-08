package com.chwihap.server.domain.feed.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 채용공고 수집 배치 트리거. 채용공고는 실시간 갱신이 불필요하여 하루 2회(08시·20시) 수집한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobFeedSyncScheduler {

    private final JobFeedSyncService jobFeedSyncService;

    @Scheduled(cron = "0 0 8,20 * * *")
    public void sync() {
        log.info("공고 수집 스케줄러 시작");
        jobFeedSyncService.sync();
    }
}
