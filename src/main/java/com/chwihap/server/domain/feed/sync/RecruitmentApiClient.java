package com.chwihap.server.domain.feed.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * data.go.kr 공공기관 채용정보 API의 목록 조회(`GET /list`)를 호출한다.
 * 페이지 단위로 최대 {@link #MAX_ATTEMPTS}회 재시도하고, 모두 실패하면 {@link RecruitmentApiException}을 던진다.
 */
@Slf4j
@Component
public class RecruitmentApiClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final String LIST_PATH = "/list";
    private static final String RESULT_TYPE_JSON = "json";

    private final RestClient recruitmentRestClient;
    private final DataGoKrProperties properties;

    public RecruitmentApiClient(RestClient recruitmentRestClient, DataGoKrProperties properties) {
        this.recruitmentRestClient = recruitmentRestClient;
        this.properties = properties;
    }

    /**
     * 지정 페이지의 채용공고 목록을 조회한다.
     *
     * @param pageNo    페이지 번호 (1부터 시작)
     * @param numOfRows 페이지당 조회 건수
     * @return API 응답
     * @throws RecruitmentApiException 재시도 후에도 호출에 실패한 경우
     */
    public RecruitmentResponse fetchPage(int pageNo, int numOfRows) {
        URI uri = buildListUri(pageNo, numOfRows);

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return recruitmentRestClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(RecruitmentResponse.class);
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("data.go.kr 채용정보 호출 실패 (pageNo={}, attempt={}/{}): {}",
                        pageNo, attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        throw new RecruitmentApiException(
                "data.go.kr 채용정보 호출이 " + MAX_ATTEMPTS + "회 재시도 후에도 실패 (pageNo=" + pageNo + ")", lastError);
    }

    /**
     * serviceKey(Encoding 값)는 이미 퍼센트 인코딩되어 있으므로 {@code build(true)}로 재인코딩을 막는다.
     * (재인코딩 시 %2B → %252B 형태로 깨져 인증 실패)
     */
    private URI buildListUri(int pageNo, int numOfRows) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl() + LIST_PATH)
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("resultType", RESULT_TYPE_JSON)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .build(true)
                .toUri();
    }
}
