package com.chwihap.server.domain.feed.sync;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobCategoryClassifierTest {

    private final JobCategoryClassifier classifier = new JobCategoryClassifier();

    @Test
    void 제목에_구매자재물류_키워드만_있으면_해당_직군_하나만_반환한다() {
        String result = classifier.classify("물류센터 자재관리 담당자 모집");

        assertThat(result).isEqualTo("구매.자재.물류");
    }

    @Test
    void 제목에_여러_직군_키워드가_있으면_콤마로_이어붙여_반환한다() {
        String result = classifier.classify("백엔드 개발자 채용담당자 모집");

        assertThat(result).isEqualTo("인사.노무.HR,IT개발/데이터");
    }

    @Test
    void 매칭되는_키워드가_없으면_null을_반환한다() {
        String result = classifier.classify("2026년 지방자치단체 임용시험 시행계획 공고");

        assertThat(result).isNull();
    }

    @Test
    void 제목이_비어있으면_null을_반환한다() {
        assertThat(classifier.classify(null)).isNull();
        assertThat(classifier.classify("")).isNull();
        assertThat(classifier.classify("   ")).isNull();
    }

    @Test
    void 삭제된_직무_도메인의_옛_키워드는_더_이상_매칭되지_않는다() {
        // "청원경찰"은 기존 체계의 "법률.경찰.소방.교도.국방"이었으나 신규 매핑표에서 도메인 자체가 삭제됨.
        String result = classifier.classify("2026년 공주시 청원경찰 임용시험 시행계획 공고");

        assertThat(result).isNull();
    }

    @Test
    void 유지된_직무_도메인은_기존_키워드로_계속_분류된다() {
        String result = classifier.classify("2026년 환경직 공무원 채용 공고");

        assertThat(result).isEqualTo("환경.에너지.안전");
    }

    @Test
    void 긴_영어_키워드는_대소문자를_구분하지_않고_매칭된다() {
        assertThat(classifier.classify("Backend Engineer")).isEqualTo("IT개발/데이터");
        assertThat(classifier.classify("backend engineer")).isEqualTo("IT개발/데이터");
        assertThat(classifier.classify("Product designer")).isEqualTo("디자인");
    }

    @Test
    void 짧은_영어_약어_키워드는_대문자_정확매칭만_인정해_오탐을_피한다() {
        // "TA"를 대소문자 무시로 매칭하면 "Data", "Start" 같은 흔한 영단어 내부에
        // 우연히 끼어들어 인사.노무.HR로 오분류된다. 소문자 "ta"는 매칭되면 안 된다.
        assertThat(classifier.classify("Data Engineer")).isEqualTo("IT개발/데이터");
        assertThat(classifier.classify("Operations Associate")).isNull();
    }

    @Test
    void 신규_보강_키워드가_매칭된다() {
        assertThat(classifier.classify("서초네이처힐 외곽 청소원 모집")).isEqualTo("서비스");
        assertThat(classifier.classify("아파트 관리원 채용")).isEqualTo("서비스");
        assertThat(classifier.classify("사출성형 공장 생산직 조작원 모집")).isEqualTo("제조.생산");
        assertThat(classifier.classify("변압기 설계 경력직 채용")).isEqualTo("제조.생산");
        assertThat(classifier.classify("우체국 상시계약집배원 채용")).isEqualTo("운전.운송.배송");
        assertThat(classifier.classify("대학병원 내과 레지던트 모집")).isEqualTo("보건.의료");
        assertThat(classifier.classify("고등학교 기간제교원 채용 공고")).isEqualTo("교육");
        assertThat(classifier.classify("한림CC 골프장 캐디 모집")).isEqualTo("서비스");
    }

    @Test
    void 두번째_보강_라운드_키워드가_매칭된다() {
        assertThat(classifier.classify("한국법령정보원 시설관리 담당자 채용")).isEqualTo("서비스");
        assertThat(classifier.classify("신안컨트리클럽 캐디 모집")).isEqualTo("서비스");
        assertThat(classifier.classify("자동차 부품 반도체 장비 설치 작업자 모집")).isEqualTo("제조.생산");
        assertThat(classifier.classify("변압기 하드웨어 용접원 채용")).isEqualTo("제조.생산");
        assertThat(classifier.classify("Growth Manager 채용")).isEqualTo("영업.판매.무역");
        assertThat(classifier.classify("Digital Marketing 담당자 채용")).isEqualTo("마케팅.홍보.MD");
    }
}
