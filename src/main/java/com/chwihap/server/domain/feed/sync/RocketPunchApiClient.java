package com.chwihap.server.domain.feed.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 로켓펀치 Open API 채용공고 목록 조회(`GET /jobs`)를 호출한다.
 * 페이지 단위로 최대 {@link #MAX_ATTEMPTS}회 재시도하고, 모두 실패하면 {@link RocketPunchApiException}을 던진다.
 */
@Slf4j
@Component
public class RocketPunchApiClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final String JOBS_PATH = "/jobs";
    /**
     * 최신 등록순. 서버 정렬 옵션에 마감임박순은 없어, endAt 기준 임박도는 조회 후 애플리케이션에서 계산한다.
     */
    private static final String SORT_LATEST_FIRST = "DATE_DESC";

    private final RestClient rocketPunchRestClient;
    private final RocketPunchProperties properties;

    public RocketPunchApiClient(RestClient rocketPunchRestClient, RocketPunchProperties properties) {
        this.rocketPunchRestClient = rocketPunchRestClient;
        this.properties = properties;
    }

    /**
     * 지정 페이지의 채용공고 목록을 조회한다.
     *
     * @param pageNo   페이지 번호 (1부터 시작)
     * @param pageSize 페이지당 조회 건수
     * @return API 응답
     * @throws RocketPunchApiException 재시도 후에도 호출에 실패한 경우
     */
    public RocketPunchResponse fetchPage(int pageNo, int pageSize) {
        URI uri = buildJobsUri(pageNo, pageSize);

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return rocketPunchRestClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(RocketPunchResponse.class);
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("로켓펀치 채용정보 호출 실패 (pageNo={}, attempt={}/{}): {}",
                        pageNo, attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        throw new RocketPunchApiException(
                "로켓펀치 채용정보 호출이 " + MAX_ATTEMPTS + "회 재시도 후에도 실패 (pageNo=" + pageNo + ")", lastError);
    }

    private URI buildJobsUri(int pageNo, int pageSize) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl() + JOBS_PATH)
                .queryParam("sort", SORT_LATEST_FIRST)
                .queryParam("page", pageNo)
                .queryParam("pageSize", pageSize)
                .build(true)
                .toUri();
    }
}
