package com.chwihap.server.domain.feed.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * 잡아바 채용정보 API(`Type=json`) 응답 매핑.
 * 최상위가 서비스명({@code GGJOBABARECRUSTM}) 키로 감싸인 배열이고, 그 배열의 각 원소가
 * {@code head}(총건수·결과코드 등 메타) 또는 {@code row}(공고 목록) 중 하나만 갖는 구조다.
 * 헤더 값은 스코프에서 쓰지 않아 매핑하지 않고, {@code ignoreUnknown}으로 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobabaResponse(
        @JsonProperty("GGJOBABARECRUSTM") List<Section> sections
) {

    public List<Item> itemList() {
        if (sections == null) {
            return List.of();
        }
        return sections.stream()
                .map(Section::row)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(List.of());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(
            List<Item> row
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("ENTRPRS_NM") String entrprsNm,        // 기업명
            @JsonProperty("PBANC_CONT") String pbancCont,        // 공고명
            @JsonProperty("WORK_REGION_CD_CONT") String workRegionCdCont, // 근무지역코드
            @JsonProperty("WORK_REGION_CONT") String workRegionCont,     // 근무지역명
            @JsonProperty("CAREER_DIV") String careerDiv,        // 경력구분 (신입/경력/신입ㆍ경력 혼재/무관 등)
            @JsonProperty("RECRUT_FIELD_NM") String recrutFieldNm, // 모집분야명
            @JsonProperty("RCPT_END_DE") String rcptEndDe,       // 접수종료일 (yyyyMMdd)
            @JsonProperty("URL") String url                      // 원문 공고 URL
    ) {
    }
}
