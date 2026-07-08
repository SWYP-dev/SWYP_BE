package com.chwihap.server.global.config;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

/**
 * 실제 수집 배치(JobFeedSyncScheduler)는 cron 시각에만 동작하므로, 서비스키가 없거나
 * 배치 실행 전인 로컬 환경에서 job_feed가 비어 2.1~2.5 API를 바로 테스트하기 어려운 문제를
 * 보완하기 위해 샘플 데이터를 시딩한다. 실제 수집 데이터(platform=PUBLIC)와 unique 키가
 * 겹치지 않아 공존 가능하며, 수집기 검증이 끝나면 이 클래스는 제거한다.
 */
@Slf4j
@Component
@Profile("local")
@Order(2)
@RequiredArgsConstructor
public class JobFeedSeedInitializer implements CommandLineRunner {

    private final JobFeedRepository jobFeedRepository;

    @Override
    public void run(String... args) {
        if (jobFeedRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();

        jobFeedRepository.save(JobFeed.create("saramin-1001", "카카오", "백엔드 개발자",
                today.plusDays(15), "https://example.com/thumb1.png", "https://saramin.co.kr/1001",
                JobPlatform.SARAMIN, Set.of(CareerType.NEW), "BACKEND", "판교", "판교"));

        jobFeedRepository.save(JobFeed.create("worknet-2001", "네이버", "프론트엔드 개발자",
                today.plusDays(3), "https://example.com/thumb2.png", "https://worknet.go.kr/2001",
                JobPlatform.WORKNET, Set.of(CareerType.EXPERIENCED), "FRONTEND", "분당", "분당"));

        jobFeedRepository.save(JobFeed.create("saramin-1002", "토스", "백엔드 개발자",
                today.minusDays(1), "https://example.com/thumb3.png", "https://saramin.co.kr/1002",
                JobPlatform.SARAMIN, Set.of(CareerType.EXPERIENCED), "BACKEND", "강남", "강남"));

        jobFeedRepository.save(JobFeed.create("worknet-2002", "쿠팡", "데이터 엔지니어",
                null, "https://example.com/thumb4.png", "https://worknet.go.kr/2002",
                JobPlatform.WORKNET, Set.of(CareerType.NEW), "DATA", "송파", "송파"));

        jobFeedRepository.save(JobFeed.create("saramin-1003", "당근마켓", "iOS 개발자",
                today.plusDays(30), "https://example.com/thumb5.png", "https://saramin.co.kr/1003",
                JobPlatform.SARAMIN, Set.of(CareerType.NEW, CareerType.EXPERIENCED), "OTHER", "서초", "서초,강남"));

        log.info("job_feed 샘플 데이터 5건 생성됨");
    }
}
