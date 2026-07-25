package com.chwihap.server.domain.feed.sync;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 인사혁신처 공공취업정보 API는 NCS 직무 분류 필드가 없어, 공고 제목의 키워드를 보고
 * 공공데이터포털 "공공기관 채용정보"(platform=PUBLIC, {@code ncsCdNmLst})가 쓰는 것과
 * 같은 NCS 대분류 표기로 추정 분류한다.
 * <p>
 * PUBLIC 실데이터를 확인해보면 한 공고가 여러 NCS 대분류에 걸치는 경우 콤마로 이어붙여
 * 저장하므로({@code "사업관리,건설,기계,전기.전자,정보통신,환경.에너지.안전"} 등), 이 분류기도
 * 제목에서 매칭되는 대분류를 전부 모아 같은 방식(콤마 구분)으로 반환한다.
 * <p>
 * 제목만으로 신뢰도 높게 판단할 수 있는 키워드만 등록한다. 애매한 케이스를 넓게 잡으면
 * 잘못된 카테고리가 필터에 노출되므로({@link JobFeedPersonnelJobSyncService} career 분류와
 * 동일한 원칙), 매칭되는 키워드가 하나도 없으면 null을 반환해 호출부가 기존 fallback을
 * 쓰도록 한다.
 */
@Component
public class PersonnelJobCategoryClassifier {

    /**
     * NCS 대분류 → 제목에 포함되면 해당 대분류로 인정하는 키워드 목록.
     * 순서는 출력 시 카테고리 나열 순서로 쓰인다 (매칭 우선순위가 아니라 다중 매칭 시 정렬용).
     * 값은 PUBLIC 실데이터(§ job_feeds.category)에서 관측된 표기를 그대로 사용한다 (구분자 "."; "·" 아님).
     */
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = buildCategoryKeywords();

    private static Map<String, List<String>> buildCategoryKeywords() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("사업관리", List.of("사업관리", "프로젝트관리"));
        map.put("경영.회계.사무", List.of("사무직", "사무원", "회계직", "경리"));
        map.put("금융.보험", List.of("금융직", "보험"));
        map.put("교육.자연.사회과학", List.of("교사", "교원", "강사"));
        map.put("법률.경찰.소방.교도.국방", List.of("경찰관", "순경", "청원경찰", "소방관", "소방사", "소방교", "교정직", "교도관", "군무원"));
        map.put("보건.의료", List.of("간호사", "의사", "약사", "보건직", "보건소", "임상병리사", "방사선사"));
        map.put("사회복지.종교", List.of("사회복지", "복지사"));
        map.put("문화.예술.디자인.방송", List.of("디자이너", "방송직"));
        map.put("운전.운송", List.of("운전직", "운송직", "기관사"));
        map.put("영업판매", List.of("영업직", "판매원"));
        map.put("경비.청소", List.of("경비직", "청소원", "시설관리원", "미화원"));
        map.put("이용.숙박.여행.오락.스포츠", List.of("체육직", "관광직"));
        map.put("음식서비스", List.of("조리원", "조리사", "조리실무사", "영양사", "급식"));
        map.put("건설", List.of("토목직", "건축직"));
        map.put("기계", List.of("기계직", "기계설비"));
        map.put("전기.전자", List.of("전기직", "전자직"));
        map.put("정보통신", List.of("전산직", "정보시스템", "전산사무"));
        map.put("환경.에너지.안전", List.of("환경직", "에너지직", "안전관리자"));
        map.put("농림어업", List.of("농업직", "임업직", "수산직"));
        map.put("연구", List.of("연구원", "연구사"));
        return Collections.unmodifiableMap(map);
    }

    /**
     * title에 매칭되는 NCS 대분류를 전부 모아 콤마로 이어붙여 반환한다.
     * 매칭되는 것이 하나도 없으면 null.
     */
    public String classify(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }

        List<String> matched = CATEGORY_KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(title::contains))
                .map(Map.Entry::getKey)
                .toList();

        return matched.isEmpty() ? null : String.join(",", matched);
    }
}
