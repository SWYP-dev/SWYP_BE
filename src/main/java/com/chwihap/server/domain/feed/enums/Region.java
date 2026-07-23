package com.chwihap.server.domain.feed.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채용 공고 필터용 표준 시/도 구분. data.go.kr API가 주는 지역명이 17개 시/도 목록에
 * 없으면 IT 밀집 지역 등 알려진 시/군/구·지역구 별칭({@link #DISTRICT_ALIASES})으로 한 번 더
 * 시도하고, 그래도 못 찾으면 {@link #OTHER}로 묶는다.
 */
public enum Region {
    SEOUL("서울"),
    BUSAN("부산"),
    DAEGU("대구"),
    INCHEON("인천"),
    GWANGJU("광주"),
    DAEJEON("대전"),
    ULSAN("울산"),
    SEJONG("세종"),
    GYEONGGI("경기"),
    GANGWON("강원"),
    CHUNGBUK("충북"),
    CHUNGNAM("충남"),
    JEONBUK("전북"),
    JEONNAM("전남"),
    GYEONGBUK("경북"),
    GYEONGNAM("경남"),
    JEJU("제주"),
    OTHER("기타");

    private static final Map<String, Region> BY_LABEL = Arrays.stream(values())
            .collect(Collectors.toMap(Region::getLabel, Function.identity()));

    /**
     * 전국 행정구역 전체를 다루진 않고, 실제로 관측된 IT 밀집 지역 위주로만 유지한다.
     * 새로운 지역명이 "기타"로 새는 게 확인되면 여기에 추가한다.
     */
    /**
     * 행정표준코드관리시스템의 시/도 코드(앞 2자리) → {@link Region} 매핑.
     * 인사혁신처 공공취업정보 API의 areacode(5자리 행정동코드) 정규화에 사용한다.
     */
    private static final Map<String, Region> BY_SIDO_CODE = Map.ofEntries(
            Map.entry("11", SEOUL),
            Map.entry("26", BUSAN),
            Map.entry("27", DAEGU),
            Map.entry("28", INCHEON),
            Map.entry("29", GWANGJU),
            Map.entry("30", DAEJEON),
            Map.entry("31", ULSAN),
            Map.entry("36", SEJONG),
            Map.entry("41", GYEONGGI),
            Map.entry("42", GANGWON),
            Map.entry("43", CHUNGBUK),
            Map.entry("44", CHUNGNAM),
            Map.entry("45", JEONBUK),
            Map.entry("46", JEONNAM),
            Map.entry("47", GYEONGBUK),
            Map.entry("48", GYEONGNAM),
            Map.entry("50", JEJU)
    );

    private static final Map<String, Region> DISTRICT_ALIASES = Map.ofEntries(
            Map.entry("강남", SEOUL),
            Map.entry("서초", SEOUL),
            Map.entry("송파", SEOUL),
            Map.entry("여의도", SEOUL),
            Map.entry("가산", SEOUL),
            Map.entry("구로", SEOUL),
            Map.entry("종로", SEOUL),
            Map.entry("마포", SEOUL),
            Map.entry("성수", SEOUL),
            Map.entry("잠실", SEOUL),
            Map.entry("판교", GYEONGGI),
            Map.entry("분당", GYEONGGI),
            Map.entry("일산", GYEONGGI),
            Map.entry("수원", GYEONGGI),
            Map.entry("성남", GYEONGGI)
    );

    private final String label;

    Region(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 원문 지역명을 표준 시/도 라벨로 정규화한다.
     * 표준 시/도 명칭 → 알려진 지역구 별칭 순으로 매칭하고, 둘 다 없으면 {@link #OTHER}.
     */
    public static Region fromRaw(String raw) {
        if (raw == null) {
            return OTHER;
        }
        String trimmed = raw.trim();
        Region bySido = BY_LABEL.get(trimmed);
        if (bySido != null) {
            return bySido;
        }
        return DISTRICT_ALIASES.getOrDefault(trimmed, OTHER);
    }

    /**
     * 5자리 행정동코드(areacode)의 앞 2자리(시/도 코드)로 지역을 정규화한다.
     */
    public static Region fromAreaCode(String areacode) {
        if (areacode == null || areacode.length() < 2) {
            return OTHER;
        }
        return BY_SIDO_CODE.getOrDefault(areacode.substring(0, 2), OTHER);
    }
}
