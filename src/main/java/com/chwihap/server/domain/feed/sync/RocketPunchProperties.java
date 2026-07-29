package com.chwihap.server.domain.feed.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로켓펀치 Open API 채용정보 연동 설정.
 *
 * @param baseUrl API base URL (예: https://openapi.rocketpunch.com/v1)
 * @param apiKey  앱 발급 API 키. 환경변수 ROCKETPUNCH_API_KEY로 주입하며, {@code X-RP-API-Key} 헤더로 전달한다.
 * @param sync    수집 배치 파라미터
 */
@ConfigurationProperties(prefix = "rocketpunch")
public record RocketPunchProperties(
        String baseUrl,
        String apiKey,
        Sync sync
) {

    /**
     * @param pageSize 페이지당 조회 건수 (API 상한 50)
     * @param maxPages 1회 수집 시 순회할 최대 페이지 수
     */
    public record Sync(int pageSize, int maxPages) {
    }
}
