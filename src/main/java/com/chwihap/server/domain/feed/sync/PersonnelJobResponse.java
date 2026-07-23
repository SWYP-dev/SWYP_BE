package com.chwihap.server.domain.feed.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 인사혁신처 공공취업정보 API(`GET /getList`) 응답 매핑.
 * 스코프에서 사용하는 필드만 매핑하고 나머지는 무시한다.
 * <p>
 * Java record + {@code @JacksonXmlElementWrapper}/리스트 조합은 Jackson XML 모듈에서
 * creator property 매핑이 깨지는 이슈가 있어(jackson-dataformat-xml의 record 지원 한계),
 * 필드 바인딩 방식의 일반 클래스로 매핑한다.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public class PersonnelJobResponse {

    @JacksonXmlProperty(localName = "header")
    private Header header;

    @JacksonXmlProperty(localName = "body")
    private Body body;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        @JacksonXmlProperty(localName = "resultCode")
        private String resultCode;

        @JacksonXmlProperty(localName = "resultMsg")
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JacksonXmlElementWrapper(localName = "items")
        @JacksonXmlProperty(localName = "item")
        private List<Item> items;

        @JacksonXmlProperty(localName = "numOfRows")
        private Integer numOfRows;

        @JacksonXmlProperty(localName = "pageNo")
        private Integer pageNo;

        @JacksonXmlProperty(localName = "totalCount")
        private Integer totalCount;

        public List<Item> itemList() {
            return items == null ? List.of() : items;
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JacksonXmlProperty(localName = "idx")
        private Long idx;               // 공고 고유번호(PK)

        @JacksonXmlProperty(localName = "insttname")
        private String insttname;       // 기관명

        @JacksonXmlProperty(localName = "title")
        private String title;           // 공고 제목

        @JacksonXmlProperty(localName = "enddate")
        private String enddate;         // 마감일 (yyyyMMdd)

        @JacksonXmlProperty(localName = "areacode")
        private String areacode;        // 행정동코드(5자리)

        @JacksonXmlProperty(localName = "type01")
        private String type01;          // 공고유형 (e01~e06)
    }
}
