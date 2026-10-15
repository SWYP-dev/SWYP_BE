package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JobFeedJobabaSyncService#category(String, String)}의 우선순위 규칙을 검증한다:
 * 제목 키워드 기반 신규 직군 매칭 &gt; 원본 모집분야명(RECRUT_FIELD_NM) fallback.
 * 수집 시 공고명/기업명의 HTML 엔티티 디코딩도 검증한다.
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

    @Test
    void 공고명과_기업명의_HTML_엔티티를_디코딩해서_저장한다() {
        String deadline = LocalDate.now().plusDays(7).format(DateTimeFormatter.BASIC_ISO_DATE);
        JobabaResponse.Item item = new JobabaResponse.Item(
                "(주)에이&amp;비", "&apos;26 신입 백엔드 개발자 채용", "41110", "수원시",
                "신입", "IT", deadline, "https://www.jobaba.net/posting/1");
        when(properties.serviceKey()).thenReturn("test-key");
        when(properties.sync()).thenReturn(new JobabaProperties.Sync(10, 1));
        when(jobabaApiClient.fetchPage(anyInt(), anyInt()))
                .thenReturn(new JobabaResponse(List.of(new JobabaResponse.Section(List.of(item)))));

        service.sync();

        ArgumentCaptor<JobFeed> captor = ArgumentCaptor.forClass(JobFeed.class);
        verify(jobFeedRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("'26 신입 백엔드 개발자 채용");
        assertThat(captor.getValue().getCompanyName()).isEqualTo("(주)에이&비");
    }
}
