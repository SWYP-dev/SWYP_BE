package com.chwihap.server.domain.feed.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegionTest {

    @ParameterizedTest
    @ValueSource(strings = {"서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"})
    void 표준_시도_명칭은_그대로_매핑된다(String label) {
        assertThat(Region.fromRaw(label).getLabel()).isEqualTo(label);
    }

    @ParameterizedTest
    @CsvSource({
            "강남, 서울", "서초, 서울", "송파, 서울", "여의도, 서울", "가산, 서울",
            "구로, 서울", "종로, 서울", "마포, 서울", "성수, 서울", "잠실, 서울",
            "판교, 경기", "분당, 경기", "일산, 경기", "수원, 경기", "성남, 경기"
    })
    void 알려진_지역구_별칭은_상위_시도로_매핑된다(String raw, String expectedLabel) {
        assertThat(Region.fromRaw(raw).getLabel()).isEqualTo(expectedLabel);
    }

    @ParameterizedTest
    @ValueSource(strings = {"제주시", "역삼동", "알수없는지역"})
    void 별칭_사전에도_없는_지역명은_기타로_묶인다(String raw) {
        assertThat(Region.fromRaw(raw)).isEqualTo(Region.OTHER);
    }

    @Test
    void null이면_기타를_반환한다() {
        assertThat(Region.fromRaw(null)).isEqualTo(Region.OTHER);
    }

    @Test
    void 앞뒤_공백은_무시하고_매핑한다() {
        assertThat(Region.fromRaw("  서울  ")).isEqualTo(Region.SEOUL);
    }
}
