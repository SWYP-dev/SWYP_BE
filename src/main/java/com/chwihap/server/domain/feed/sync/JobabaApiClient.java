package com.chwihap.server.domain.feed.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 경기데이터드림 「경기도일자리재단 잡아바 채용정보」API의 목록 조회를 호출한다.
 * 페이지 단위로 최대 {@link #MAX_ATTEMPTS}회 재시도하고, 모두 실패하면 {@link JobabaApiException}을 던진다.
 */
@Slf4j
@Component
public class JobabaApiClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final String RESULT_TYPE_JSON = "json";

    private final RestClient jobabaRestClient;
    private final JobabaProperties properties;

    public JobabaApiClient(RestClient jobabaRestClient, JobabaProperties properties) {
        this.jobabaRestClient = jobabaRestClient;
        this.properties = properties;
    }

    /**
     * 지정 페이지의 잡아바 채용공고 목록을 조회한다.
     *
     * @param pIndex 페이지 위치 (1부터 시작)
     * @param pSize  페이지당 조회 건수
     * @return API 응답
     * @throws JobabaApiException 재시도 후에도 호출에 실패한 경우
     */
    public JobabaResponse fetchPage(int pIndex, int pSize) {
        URI uri = buildListUri(pIndex, pSize);

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return jobabaRestClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(JobabaResponse.class);
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("잡아바 채용정보 API 호출 실패 (pIndex={}, attempt={}/{}): {}",
                        pIndex, attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        throw new JobabaApiException(
                "잡아바 채용정보 API 호출이 " + MAX_ATTEMPTS + "회 재시도 후에도 실패 (pIndex=" + pIndex + ")", lastError);
    }

    private URI buildListUri(int pIndex, int pSize) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .queryParam("KEY", properties.serviceKey())
                .queryParam("Type", RESULT_TYPE_JSON)
                .queryParam("pIndex", pIndex)
                .queryParam("pSize", pSize)
                .build(true)
                .toUri();
    }
}
