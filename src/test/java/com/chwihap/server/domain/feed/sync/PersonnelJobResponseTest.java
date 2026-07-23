package com.chwihap.server.domain.feed.sync;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonnelJobResponseTest {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Test
    void 실제_응답_샘플_XML을_파싱한다() throws Exception {
        String xml = """
                <response>
                <header>
                <resultCode>00</resultCode>
                <resultMsg>NORMAL SERVICE.</resultMsg>
                </header>
                <body>
                <items>
                <item>
                <areacode>11440</areacode>
                <enddate>20260121</enddate>
                <idx>275459</idx>
                <insttname>경찰청 서울특별시경찰청 광역수사단 반부패수사대</insttname>
                <moddate>20260112</moddate>
                <readnum>508</readnum>
                <regdate>20260112</regdate>
                <title>서울경찰청 공무직 근로자(기간제,시설분야) 채용 공고</title>
                <type01>e01</type01>
                <type02>g01</type02>
                </item>
                <item>
                <areacode>11440</areacode>
                <enddate>20260213</enddate>
                <idx>281207</idx>
                <insttname>경찰청 서울특별시경찰청 광역수사단 반부패수사대</insttname>
                <moddate>20260202</moddate>
                <readnum>301</readnum>
                <regdate>20260130</regdate>
                <title>서울경찰청 공무직 근로자(기간제, 시설분야) 채용 공고</title>
                <type01>e01</type01>
                <type02>g01</type02>
                </item>
                </items>
                <numOfRows>10</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>2</totalCount>
                </body>
                </response>
                """;

        PersonnelJobResponse response = xmlMapper.readValue(xml, PersonnelJobResponse.class);

        assertThat(response.getHeader().getResultCode()).isEqualTo("00");
        assertThat(response.getBody().getTotalCount()).isEqualTo(2);
        assertThat(response.getBody().itemList()).hasSize(2);

        PersonnelJobResponse.Item first = response.getBody().itemList().get(0);
        assertThat(first.getIdx()).isEqualTo(275459L);
        assertThat(first.getInsttname()).isEqualTo("경찰청 서울특별시경찰청 광역수사단 반부패수사대");
        assertThat(first.getTitle()).isEqualTo("서울경찰청 공무직 근로자(기간제,시설분야) 채용 공고");
        assertThat(first.getEnddate()).isEqualTo("20260121");
        assertThat(first.getAreacode()).isEqualTo("11440");
        assertThat(first.getType01()).isEqualTo("e01");
    }

    @Test
    void items가_없는_페이지는_빈_리스트를_반환한다() throws Exception {
        String xml = """
                <response>
                <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
                <body>
                <numOfRows>10</numOfRows>
                <pageNo>99</pageNo>
                <totalCount>2</totalCount>
                </body>
                </response>
                """;

        PersonnelJobResponse response = xmlMapper.readValue(xml, PersonnelJobResponse.class);

        assertThat(response.getBody().itemList()).isEmpty();
    }
}
