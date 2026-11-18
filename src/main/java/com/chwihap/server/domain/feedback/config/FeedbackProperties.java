package com.chwihap.server.domain.feedback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.feedback")
public class FeedbackProperties {

    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {
        // window 동안 사용자 ID 또는 IP별로 허용하는 최대 제출 횟수
        private int maxRequests = 5;
        private Duration window = Duration.ofMinutes(10);
    }
}
