package com.chwihap.server.domain.feed.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 경기데이터드림 「경기도일자리재단 잡아바 채용정보」 Open API 연동 설정.
 *
 * @param baseUrl    API base URL (예: https://openapi.gg.go.kr/GGJOBABARECRUSTM)
 * @param serviceKey 인증키. 환경변수 GG_DATA_DREAM_SERVICE_KEY로 주입한다.
 * @param sync       수집 배치 파라미터
 */
@ConfigurationProperties(prefix = "gg-data-dream.jobaba")
public record JobabaProperties(
        String baseUrl,
        String serviceKey,
        Sync sync
) {

    /**
     * @param numOfRows 페이지당 조회 건수(pSize, 최대 1,000)
     * @param maxPages  1회 수집 시 순회할 최대 페이지 수
     */
    public record Sync(int numOfRows, int maxPages) {
    }
}
