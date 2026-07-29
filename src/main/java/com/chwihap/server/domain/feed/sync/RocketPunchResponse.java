package com.chwihap.server.domain.feed.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 로켓펀치 Open API 채용공고 목록(`GET /v1/jobs`) 응답 매핑.
 * 스코프에서 사용하는 필드만 매핑하고 나머지는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RocketPunchResponse(
        @JsonProperty("totalItems") Long totalItems,
        @JsonProperty("totalPages") Integer totalPages,
        @JsonProperty("page") Integer page,
        @JsonProperty("pageSize") Integer pageSize,
        @JsonProperty("items") List<Item> items
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("jobId") Long jobId,                     // 채용공고 ID(PK)
            @JsonProperty("title") String title,                   // 공고 제목
            @JsonProperty("jobCategory") String jobCategory,       // 직군 코드 (예: dev, dataAi)
            @JsonProperty("seniorities") List<String> seniorities, // 숙련도 목록 (BEGINNER/JUNIOR/MIDLEVEL/SENIOR/EXECUTIVE)
            @JsonProperty("company") Company company,
            @JsonProperty("endAt") String endAt,                   // 모집 종료(ISO LocalDateTime 문자열), 상시채용이면 null
            @JsonProperty("webUrl") String webUrl                  // 로켓펀치 공고 페이지 URL
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Company(
            @JsonProperty("name") String name,
            @JsonProperty("logoUrl") String logoUrl
    ) {
    }
}
