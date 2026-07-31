package com.chwihap.server.domain.feed.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobabaResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 실제_응답_샘플_JSON을_파싱한다() throws Exception {
        String json = """
                {
                  "GGJOBABARECRUSTM": [
                    {
                      "head": [
                        { "list_total_count": 147592 },
                        { "RESULT": { "CODE": "INFO-000", "MESSAGE": "정상 처리되었습니다." } },
                        { "api_version": "1.0" }
                      ]
                    },
                    {
                      "row": [
                        {
                          "ENTRPRS_NM": "(주)케이탑인터내셔널",
                          "PBANC_CONT": "신입 사무장님 모집 합니다.",
                          "WORK_REGION_CD_CONT": "4146",
                          "WORK_REGION_CONT": "용인시",
                          "CAREER_DIV": "신입",
                          "RECRUT_FIELD_NM": "영업ㆍ판매ㆍ운전ㆍ운송직",
                          "RCPT_BGNG_DE": "20260529",
                          "RCPT_END_DE": "20260628",
                          "URL": "https://www.jobkorea.co.kr/Recruit/GI_Read/49276193?Oem_Code=C900&api=232"
                        }
                      ]
                    }
                  ]
                }
                """;

        JobabaResponse response = objectMapper.readValue(json, JobabaResponse.class);

        assertThat(response.itemList()).hasSize(1);
        JobabaResponse.Item item = response.itemList().get(0);
        assertThat(item.entrprsNm()).isEqualTo("(주)케이탑인터내셔널");
        assertThat(item.pbancCont()).isEqualTo("신입 사무장님 모집 합니다.");
        assertThat(item.workRegionCdCont()).isEqualTo("4146");
        assertThat(item.careerDiv()).isEqualTo("신입");
        assertThat(item.rcptEndDe()).isEqualTo("20260628");
        assertThat(item.url()).isEqualTo("https://www.jobkorea.co.kr/Recruit/GI_Read/49276193?Oem_Code=C900&api=232");
    }

    @Test
    void row가_없는_head만_있는_페이지는_빈_리스트를_반환한다() throws Exception {
        String json = """
                {
                  "GGJOBABARECRUSTM": [
                    {
                      "head": [
                        { "list_total_count": 0 },
                        { "RESULT": { "CODE": "INFO-200", "MESSAGE": "해당하는 데이터가 없습니다." } }
                      ]
                    }
                  ]
                }
                """;

        JobabaResponse response = objectMapper.readValue(json, JobabaResponse.class);

        assertThat(response.itemList()).isEmpty();
    }
}
