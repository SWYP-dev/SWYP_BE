package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.repository.JobFeedRepository;
import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobFeedChangeRepository;
import com.chwihap.server.domain.feed.service.JobFeedUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
                mock(PlatformTransactionManager.class),
                mock(JobFeedUpdateService.class));
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

    @Test
    void 기존_공고의_디코딩된_값을_갱신하고_변경_이력을_저장한다() {
        LocalDate deadline = LocalDate.now().plusDays(10);
        String url = "https://www.gojobs.go.kr/apmView.do?empmnsn=123";
        JobFeed feed = JobFeed.create("123", "기존 회사", "기존 제목", deadline.minusDays(1),
                null, url, JobPlatform.PUBLIC_PERSONNEL, Set.of(), null, null, null);
        PersonnelJobResponse.Item item = new PersonnelJobResponse.Item();
        ReflectionTestUtils.setField(item, "idx", 123L);
        ReflectionTestUtils.setField(item, "insttname", "R&amp;D 기관");
        ReflectionTestUtils.setField(item, "title", "&apos;신입&apos; 채용");
        ReflectionTestUtils.setField(item, "type01", "e01");
        ReflectionTestUtils.setField(item, "enddate",
                deadline.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
        when(jobFeedRepository.findByPlatformAndExternalIdIn(JobPlatform.PUBLIC_PERSONNEL, List.of("123")))
                .thenReturn(List.of(feed));
        JobFeedChangeRepository changes = mock(JobFeedChangeRepository.class);
        JobFeedPersonnelJobSyncService syncService = new JobFeedPersonnelJobSyncService(
                jobFeedRepository, personnelJobApiClient, properties, new JobCategoryClassifier(),
                mock(PlatformTransactionManager.class), new JobFeedUpdateService(changes));

        ReflectionTestUtils.invokeMethod(syncService, "upsertItems", List.of(item));

        ArgumentCaptor<JobFeedChange> captor = ArgumentCaptor.forClass(JobFeedChange.class);
        verify(changes).save(captor.capture());
        assertThat(captor.getValue().getOldCompanyName()).isEqualTo("기존 회사");
        assertThat(captor.getValue().getNewCompanyName()).isEqualTo("R&D 기관");
        assertThat(captor.getValue().getOldDeadline()).isEqualTo(deadline.minusDays(1));
        assertThat(captor.getValue().getNewDeadline()).isEqualTo(deadline);
        assertThat(feed.getCompanyName()).isEqualTo("R&D 기관");
        assertThat(feed.getTitle()).isEqualTo("'신입' 채용");
        assertThat(feed.getDeadline()).isEqualTo(deadline);
    }
}
