package com.chwihap.server.domain.feedback.service;

import com.chwihap.server.domain.feedback.config.FeedbackProperties;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 비로그인에도 열려 있는 의견 제출 API의 남용을 막기 위해 사용자 ID(비로그인은 IP)별 제출 횟수를 제한한다.
 * Redis 장애 시에는 정상 제출까지 막지 않도록 제한 없이 통과시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackRateLimiter {

    private static final String KEY_PREFIX = "feedback:submit:";

    private final RedissonClient redissonClient;
    private final FeedbackProperties properties;

    public void checkSubmit(Long userId, String clientIp) {
        String key = userId != null ? KEY_PREFIX + "user:" + userId : KEY_PREFIX + "ip:" + clientIp;
        boolean acquired;
        try {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
            Duration window = properties.getRateLimit().getWindow();
            // 이미 설정된 키면 유지된다. 마지막 사용 후 window가 지나면 키가 만료된다.
            rateLimiter.trySetRate(RateType.OVERALL, properties.getRateLimit().getMaxRequests(), window, window);
            acquired = rateLimiter.tryAcquire();
        } catch (RuntimeException e) {
            log.warn("의견 제출 Rate limit 확인 실패로 제한 없이 통과합니다. key={}", key, e);
            return;
        }
        if (!acquired) {
            throw new BusinessException(ErrorCode.FEEDBACK_RATE_LIMITED);
        }
    }
}
