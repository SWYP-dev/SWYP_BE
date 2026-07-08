package com.chwihap.server.domain.feed.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * data.go.kr 공공기관 채용정보 API(`GET /list`) 응답 매핑.
 * 스코프에서 사용하는 필드만 매핑하고 나머지는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecruitmentResponse(
        @JsonProperty("resultCode") Integer resultCode,
        @JsonProperty("resultMsg") String resultMsg,
        @JsonProperty("totalCount") Integer totalCount,
        @JsonProperty("result") List<Item> result
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("recrutPblntSn") Long recrutPblntSn,   // 채용공고 일련번호(PK)
            @JsonProperty("instNm") String instNm,               // 기관명
            @JsonProperty("recrutPbancTtl") String recrutPbancTtl, // 공고 제목
            @JsonProperty("pbancEndYmd") String pbancEndYmd,     // 마감일 (문자열)
            @JsonProperty("srcUrl") String srcUrl,               // 원본 공고 URL
            @JsonProperty("workRgnNmLst") String workRgnNmLst,   // 근무지역명 (콤마 구분 가능)
            @JsonProperty("ncsCdNmLst") String ncsCdNmLst,       // NCS 직무 대분류명
            @JsonProperty("recrutSeNm") String recrutSeNm,       // 채용구분 (신입/경력/신입+경력)
            @JsonProperty("ongoingYn") String ongoingYn          // 진행중 여부 (Y/N)
    ) {
    }
}
