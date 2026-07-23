package com.chwihap.server.domain.feed.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인사혁신처 공공취업정보 API(data.go.kr) 연동 설정.
 *
 * @param baseUrl    API base URL (예: https://apis.data.go.kr/1760000/PblJobService)
 * @param serviceKey 서비스 인증키(Encoding 값). 환경변수 DATA_GO_KR_PERSONNEL_SERVICE_KEY로 주입한다.
 * @param sync       수집 배치 파라미터
 */
@ConfigurationProperties(prefix = "data-go-kr.personnel-job")
public record PersonnelJobProperties(
        String baseUrl,
        String serviceKey,
        Sync sync
) {

    /**
     * @param numOfRows 페이지당 조회 건수
     * @param maxPages  1회 수집 시 순회할 최대 페이지 수
     */
    public record Sync(int numOfRows, int maxPages) {
    }
}
