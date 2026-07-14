package com.chwihap.server.domain.document.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3DocumentCleanupScheduler {

    private static final String LOCK_KEY = "lock:s3-document-cleanup";
    private static final long LEASE_TIME_MINUTES = 30;

    private final S3DocumentCleanupService S3DocumentCleanupService;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteSoftDeletedFiles() {
        RLock lock = redissonClient.getLock(LOCK_KEY);  // 락 객체 준비
        boolean acquired = false;   // 현재 인스턴스 락 획득 여부

        try {
            acquired = lock.tryLock(0, LEASE_TIME_MINUTES, TimeUnit.MINUTES);   // 락을 획득 시도

            if (!acquired) {    // 락을 얻었는지 확인
                log.info("다른 인스턴스가 S3 파일을 정리 중이라 이번 실행은 스킵합니다.");
                return; // 락이 없기 때문에 해당 인스턴스 실행 중지
            }
            log.info("S3 파일 정리 스케줄러 시작");
            S3DocumentCleanupService.deleteSoftDeletedFiles();  // S3 삭제 및 DB 변경
            log.info("S3 파일 정리 스케줄러 완료");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("S3 파일 정리 락 획득 중 인터럽트가 발생하였습니다.", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                // 락을 획득하고 락 해제
                lock.unlock();
            }
        }
    }
}
