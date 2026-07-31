package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link JobFeedJobabaSyncService#category(String, String)}의 우선순위 규칙을 검증한다:
 * 제목 키워드 기반 신규 직군 매칭 &gt; 원본 모집분야명(RECRUT_FIELD_NM) fallback.
 */
@ExtendWith(MockitoExtension.class)
class JobFeedJobabaSyncServiceTest {

    @Mock
    private JobFeedRepository jobFeedRepository;
    @Mock
    private JobabaApiClient jobabaApiClient;
    @Mock
    private JobabaProperties properties;

    private JobFeedJobabaSyncService service;

    @BeforeEach
    void setUp() {
        service = new JobFeedJobabaSyncService(
                jobFeedRepository,
                jobabaApiClient,
                properties,
                new JobCategoryClassifier(),
                mock(PlatformTransactionManager.class));
    }

    @Test
    void 제목에서_신규_직군이_확인되면_모집분야명_대신_신규_직군을_반환한다() {
        String category = service.category("신입 백엔드 개발자 채용", "영업ㆍ판매ㆍ운전ㆍ운송직");

        assertThat(category).isEqualTo("IT개발/데이터");
    }

    @Test
    void 제목에서_신규_직군을_추정할_수_없으면_원본_모집분야명을_반환한다() {
        String category = service.category("2026년 상반기 정기 채용 안내", "미용ㆍ여행ㆍ숙박ㆍ음식ㆍ경비ㆍ청소직");

        assertThat(category).isEqualTo("미용ㆍ여행ㆍ숙박ㆍ음식ㆍ경비ㆍ청소직");
    }

    @Test
    void 제목_매칭도_모집분야명도_없으면_null을_반환한다() {
        String category = service.category("공고", null);

        assertThat(category).isNull();
    }
}
