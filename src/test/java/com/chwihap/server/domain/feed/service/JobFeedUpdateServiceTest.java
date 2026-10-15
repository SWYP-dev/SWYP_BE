package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobFeedChangeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobFeedUpdateServiceTest {
    @Mock
    private JobFeedChangeRepository repository;

    @Test
    void 원본을_덮어쓰기_전에_변경값을_캡처한다() {
        JobFeed feed = feed();
        new JobFeedUpdateService(repository).update(feed, "새 회사", "새 제목", null,
                null, "https://example.com/new", Set.of(), null, null, null);
        ArgumentCaptor<JobFeedChange> captor = ArgumentCaptor.forClass(JobFeedChange.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOldCompanyName()).isEqualTo("기존 회사");
        assertThat(captor.getValue().getNewCompanyName()).isEqualTo("새 회사");
        assertThat(feed.getCompanyName()).isEqualTo("새 회사");
    }

    @Test
    void 제목만_바뀌면_원본은_갱신하지만_알림용_이력은_생성하지_않는다() {
        JobFeed feed = feed();
        new JobFeedUpdateService(repository).update(feed, "기존 회사", "새 제목", null,
                null, null, Set.of(), null, null, null);
        verify(repository, never()).save(any());
        assertThat(feed.getTitle()).isEqualTo("새 제목");
    }

    private JobFeed feed() {
        return JobFeed.create("source-1", "기존 회사", "제목", null, null, null,
                JobPlatform.PUBLIC, Set.of(), null, null, null);
    }
}
