package com.chwihap.server.domain.feedback.service;

import com.chwihap.server.domain.feedback.config.FeedbackProperties;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackRateLimiterTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RRateLimiter rateLimiter;

    private FeedbackRateLimiter feedbackRateLimiter;

    @BeforeEach
    void setUp() {
        FeedbackProperties properties = new FeedbackProperties();
        properties.getRateLimit().setMaxRequests(5);
        properties.getRateLimit().setWindow(Duration.ofMinutes(10));
        feedbackRateLimiter = new FeedbackRateLimiter(redissonClient, properties);
    }

    @Test
    void 로그인_사용자는_사용자_ID_키로_제한한다() {
        given(redissonClient.getRateLimiter("feedback:submit:user:1")).willReturn(rateLimiter);
        given(rateLimiter.tryAcquire()).willReturn(true);

        feedbackRateLimiter.checkSubmit(1L, "127.0.0.1");

        verify(rateLimiter).trySetRate(RateType.OVERALL, 5, Duration.ofMinutes(10), Duration.ofMinutes(10));
    }

    @Test
    void 비로그인_사용자는_IP_키로_제한한다() {
        given(redissonClient.getRateLimiter("feedback:submit:ip:127.0.0.1")).willReturn(rateLimiter);
        given(rateLimiter.tryAcquire()).willReturn(true);

        assertThatCode(() -> feedbackRateLimiter.checkSubmit(null, "127.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void 허용량을_초과하면_예외가_발생한다() {
        given(redissonClient.getRateLimiter("feedback:submit:user:1")).willReturn(rateLimiter);
        given(rateLimiter.tryAcquire()).willReturn(false);

        assertThatThrownBy(() -> feedbackRateLimiter.checkSubmit(1L, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEEDBACK_RATE_LIMITED);
    }

    @Test
    void Redis_오류가_나도_제출을_막지_않는다() {
        given(redissonClient.getRateLimiter("feedback:submit:user:1")).willReturn(rateLimiter);
        given(rateLimiter.tryAcquire()).willThrow(new RuntimeException("redis down"));

        assertThatCode(() -> feedbackRateLimiter.checkSubmit(1L, "127.0.0.1")).doesNotThrowAnyException();
    }
}
