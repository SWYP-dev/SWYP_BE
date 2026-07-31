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
}
