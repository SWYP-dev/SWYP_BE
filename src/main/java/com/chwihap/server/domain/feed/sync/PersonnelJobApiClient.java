package com.chwihap.server.domain.feed.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 인사혁신처 공공취업정보 API의 목록 조회(`GET /getList`)를 호출한다.
 * 페이지 단위로 최대 {@link #MAX_ATTEMPTS}회 재시도하고, 모두 실패하면 {@link PersonnelJobApiException}을 던진다.
 */
@Slf4j
@Component
public class PersonnelJobApiClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final String LIST_PATH = "/getList";
    /**
     * Sort_order=2가 등록일(regdate) 내림차순(최신순). 기본값(미지정 또는 1)은 오름차순(2008년대
     * 옛 공고부터)이라 반드시 지정해야 최신 공고 위주로 수집된다.
     */
    private static final String SORT_ORDER_LATEST_FIRST = "2";

    private final RestClient personnelJobRestClient;
    private final PersonnelJobProperties properties;

    public PersonnelJobApiClient(RestClient personnelJobRestClient, PersonnelJobProperties properties) {
        this.personnelJobRestClient = personnelJobRestClient;
        this.properties = properties;
    }

    /**
     * 지정 페이지의 공공취업정보 목록을 조회한다.
     *
     * @param pageNo    페이지 번호 (1부터 시작)
     * @param numOfRows 페이지당 조회 건수
     * @return API 응답
     * @throws PersonnelJobApiException 재시도 후에도 호출에 실패한 경우
     */
    public PersonnelJobResponse fetchPage(int pageNo, int numOfRows) {
        URI uri = buildListUri(pageNo, numOfRows);

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return personnelJobRestClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(PersonnelJobResponse.class);
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("공공취업정보 API 호출 실패 (pageNo={}, attempt={}/{}): {}",
                        pageNo, attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        throw new PersonnelJobApiException(
                "공공취업정보 API 호출이 " + MAX_ATTEMPTS + "회 재시도 후에도 실패 (pageNo=" + pageNo + ")", lastError);
    }

    /**
     * serviceKey(Encoding 값)는 이미 퍼센트 인코딩되어 있으므로 {@code build(true)}로 재인코딩을 막는다.
     * (재인코딩 시 %2B → %252B 형태로 깨져 인증 실패)
     */
    private URI buildListUri(int pageNo, int numOfRows) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl() + LIST_PATH)
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("Sort_order", SORT_ORDER_LATEST_FIRST)
                .build(true)
                .toUri();
    }
}
