package com.chwihap.server.domain.feed.sync;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 로켓펀치 {@code jobCategory} 코드(`/v1/codes/job-categories`에서 확인한 23개 고정 코드)를
 * 공공데이터포털 "공공기관 채용정보"(platform=PUBLIC, {@code ncsCdNmLst})가 쓰는 NCS 대분류
 * 표기로 최대한 근접 매핑한다. 로켓펀치는 "dev" 하나로 백엔드/프론트/데이터를 구분하지 않으므로
 * 세부 매핑이 아닌 근사치이며, 매칭되는 코드가 없으면 null을 반환해 category 없이 저장한다.
 */
@Component
public class RocketPunchCategoryMapper {

    private static final Map<String, String> CATEGORY_BY_CODE = Map.ofEntries(
            Map.entry("dev", "정보통신"),
            Map.entry("dataAi", "정보통신"),
            Map.entry("engineer", "정보통신"),
            Map.entry("rnd", "연구"),
            Map.entry("designUX", "문화.예술.디자인.방송"),
            Map.entry("media", "문화.예술.디자인.방송"),
            Map.entry("marketingPr", "영업판매"),
            Map.entry("salesCs", "영업판매"),
            Map.entry("strategy", "경영.회계.사무"),
            Map.entry("manage", "경영.회계.사무"),
            Map.entry("hr", "경영.회계.사무"),
            Map.entry("finance", "금융.보험"),
            Map.entry("invest", "금융.보험"),
            Map.entry("legal", "법률.경찰.소방.교도.국방"),
            Map.entry("edu", "교육.자연.사회과학"),
            Map.entry("medical", "보건.의료"),
            Map.entry("construct", "건설"),
            Map.entry("manufact", "기계"),
            Map.entry("logistics", "운전.운송"),
            Map.entry("food", "음식서비스"),
            Map.entry("leisure", "이용.숙박.여행.오락.스포츠"),
            Map.entry("service", "이용.숙박.여행.오락.스포츠"),
            Map.entry("clergy", "사회복지.종교")
    );

    /**
     * jobCategory 코드를 NCS 대분류 표기로 변환한다. 알려지지 않은 코드면 null.
     */
    public String map(String jobCategoryCode) {
        if (!StringUtils.hasText(jobCategoryCode)) {
            return null;
        }
        return CATEGORY_BY_CODE.get(jobCategoryCode.trim());
    }
}
