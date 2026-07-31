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
 * {@link JobFeedPersonnelJobSyncService#category(String, String, String)}의 우선순위 규칙을 검증한다:
 * 상시채용 sentinel &gt; 제목 키워드 기반 신규 직군 매칭 &gt; type01 라벨 fallback.
 */
@ExtendWith(MockitoExtension.class)
class JobFeedPersonnelJobSyncServiceTest {

    private static final String NO_DEADLINE_SENTINEL = "99991231";

    @Mock
    private JobFeedRepository jobFeedRepository;
    @Mock
    private PersonnelJobApiClient personnelJobApiClient;
    @Mock
    private PersonnelJobProperties properties;

    private JobFeedPersonnelJobSyncService service;

    @BeforeEach
    void setUp() {
        service = new JobFeedPersonnelJobSyncService(
                jobFeedRepository,
                personnelJobApiClient,
                properties,
                new JobCategoryClassifier(),
                mock(PlatformTransactionManager.class));
    }

    @Test
    void 마감일이_상시채용_sentinel이면_제목과_무관하게_상시채용을_반환한다() {
        String category = service.category("보건소 간호사 채용 공고", "e01", NO_DEADLINE_SENTINEL);

        assertThat(category).isEqualTo("상시채용");
    }

    @Test
    void 제목에서_신규_직군이_확인되면_type01_라벨_대신_신규_직군을_반환한다() {
        String category = service.category("보건소 간호사 채용 공고", "e01", "20260101");

        assertThat(category).isEqualTo("보건.의료");
    }

    @Test
    void 제목에서_신규_직군을_추정할_수_없으면_기존_type01_라벨을_반환한다() {
        String category = service.category("지방자치단체 임용시험 시행계획 공고", "e01", "20260101");

        assertThat(category).isEqualTo("공개경쟁채용");
    }

    @Test
    void type01도_매핑에_없고_신규_직군도_없으면_null을_반환한다() {
        String category = service.category("지방자치단체 임용시험 시행계획 공고", "e99", "20260101");

        assertThat(category).isNull();
    }
}
