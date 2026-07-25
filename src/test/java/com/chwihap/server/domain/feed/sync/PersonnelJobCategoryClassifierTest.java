package com.chwihap.server.domain.feed.sync;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonnelJobCategoryClassifierTest {

    private final PersonnelJobCategoryClassifier classifier = new PersonnelJobCategoryClassifier();

    @Test
    void 제목에_보건의료_키워드가_있으면_보건의료_카테고리를_반환한다() {
        String result = classifier.classify("2026년 보건소 간호사 채용 공고");

        assertThat(result).isEqualTo("보건.의료");
    }

    @Test
    void 제목에_여러_카테고리_키워드가_있으면_콤마로_이어붙여_반환한다() {
        String result = classifier.classify("전산직 및 사무직 공무직 채용 공고");

        assertThat(result).isEqualTo("경영.회계.사무,정보통신");
    }

    @Test
    void 매칭되는_키워드가_없으면_null을_반환한다() {
        String result = classifier.classify("2026년 지방자치단체 임기제공무원 채용시험 공고");

        assertThat(result).isNull();
    }

    @Test
    void 제목이_비어있으면_null을_반환한다() {
        assertThat(classifier.classify(null)).isNull();
        assertThat(classifier.classify("")).isNull();
        assertThat(classifier.classify("   ")).isNull();
    }

    @Test
    void 조리원_키워드는_음식서비스로_분류된다() {
        String result = classifier.classify("2026년 제2회 공무직(조리원) 채용공고");

        assertThat(result).isEqualTo("음식서비스");
    }

    @Test
    void 소속기관명에만_경찰_소방이_들어간_경우_법률경찰소방으로_오분류하지_않는다() {
        // 실제 직무는 환경미화(청소)인데, 채용기관명("서울특별시경찰청")에 "경찰"이 포함되어
        // 예전 키워드("경찰")로는 법률.경찰.소방.교도.국방으로 잘못 분류되던 케이스.
        String result = classifier.classify("서울특별시경찰청 청사관리계 무기계약 공무직(환경미화)면접결과 및 최종 합격자 안내");

        assertThat(result).isNull();
    }

    @Test
    void 조리실무사_키워드는_음식서비스로_분류된다() {
        String result = classifier.classify("2026학년도 동북초등학교 교육공무직원(조리실무사) 채용 공고");

        assertThat(result).isEqualTo("음식서비스");
    }

    @Test
    void 청원경찰_같은_실제_직무명은_법률경찰소방으로_정상_분류된다() {
        String result = classifier.classify("2026년 공주시 청원경찰 임용시험 시행계획 공고");

        assertThat(result).isEqualTo("법률.경찰.소방.교도.국방");
    }
}
