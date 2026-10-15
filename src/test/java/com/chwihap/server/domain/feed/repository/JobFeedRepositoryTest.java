package com.chwihap.server.domain.feed.repository;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 피드 키워드 검색 쿼리가 띄어쓰기·대소문자와 관계없이 매칭되고, LIKE 와일드카드를 문자 그대로 비교하는지 실제 DB로 검증한다.
 * keyword 인자는 FeedService가 정규화한 형태(공백 제거, 와일드카드 escape)로 넘긴다.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobFeedRepositoryTest {

    private static final List<JobPlatform> ALL_PLATFORMS = Arrays.asList(JobPlatform.values());

    @Autowired
    private JobFeedRepository jobFeedRepository;

    @BeforeEach
    void setUp() {
        save("spaced", "카카오", "백엔드 개발자 채용");
        save("nospace", "네이버", "백엔드개발자 신입");
        save("special-space", "라인", "백엔드\t개발자\u3000경력");
        save("english", "Toss Bank", "Backend Engineer");
        save("percent", "삼성 전자", "목표 100% 달성 인센티브");
    }

    @Test
    void 띄어쓰기_유무와_관계없이_같은_공고가_검색된다() {
        assertThat(searchLatest("백엔드개발자")).containsExactlyInAnyOrder("spaced", "nospace", "special-space");
        assertThat(searchDeadline("백엔드개발자")).containsExactlyInAnyOrder("spaced", "nospace", "special-space");
    }

    @Test
    void 대소문자와_관계없이_검색된다() {
        assertThat(searchLatest("backendengineer")).containsExactly("english");
        assertThat(searchLatest("BACKENDENGINEER")).containsExactly("english");
    }

    @Test
    void 회사명도_띄어쓰기와_관계없이_검색된다() {
        assertThat(searchLatest("삼성전자")).containsExactly("percent");
        assertThat(searchLatest("tossbank")).containsExactly("english");
    }

    @Test
    void escape된_LIKE_와일드카드는_문자_그대로_비교한다() {
        assertThat(searchLatest("100!%")).containsExactly("percent");
        assertThat(searchLatest("!_")).isEmpty();
    }

    private List<String> searchLatest(String keyword) {
        return jobFeedRepository.findLatestPage(ALL_PLATFORMS, false, List.of(""), false, List.of(CareerType.NEW),
                        false, List.of(""), false, LocalDate.now(), LocalDate.now(), false, keyword,
                        List.of(JobPlatform.PUBLIC), PageRequest.of(0, 20))
                .map(JobFeed::getExternalId)
                .getContent();
    }

    private List<String> searchDeadline(String keyword) {
        return jobFeedRepository.findDeadlinePage(ALL_PLATFORMS, false, List.of(""), false, List.of(CareerType.NEW),
                        false, List.of(""), false, LocalDate.now(), LocalDate.now(), false, keyword,
                        PageRequest.of(0, 20))
                .map(JobFeed::getExternalId)
                .getContent();
    }

    private void save(String externalId, String companyName, String title) {
        jobFeedRepository.save(JobFeed.create(externalId, companyName, title, LocalDate.now().plusDays(5),
                null, null, JobPlatform.JOBABA, Set.of(CareerType.NEW), null, null, null));
    }
}
