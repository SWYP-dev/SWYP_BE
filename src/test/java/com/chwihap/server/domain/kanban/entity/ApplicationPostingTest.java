package com.chwihap.server.domain.kanban.entity;

import com.chwihap.server.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApplicationPostingTest {

    @Test
    void 직접_등록_URL은_양쪽_공백만_제거한다() {
        String url = "  https://www.saramin.co.kr/zf_user/jobs/relay/pop-view"
                + "?rec_idx=51534033&inner_campaign=pop_view_12"
                + "&referNonce=584d7871d2a375330901&view_type=etc#detail  ";

        ApplicationPosting posting = ApplicationPosting.createDirect(
                mock(User.class),
                "회사",
                "백엔드 개발자",
                LocalDate.of(2026, 8, 31),
                url
        );

        assertThat(posting.getOriginalUrl()).isEqualTo(url.trim());
    }

    @Test
    void 직접_등록_URL을_수정해도_쿼리와_fragment와_후행_slash를_유지한다() {
        ApplicationPosting posting = ApplicationPosting.createDirect(
                mock(User.class),
                "회사",
                "백엔드 개발자",
                null,
                "https://example.com/jobs/1"
        );
        String updatedUrl = "  https://example.com/jobs/1/?utm_source=test#apply  ";

        posting.updateDetails("회사", "백엔드 개발자", null, updatedUrl);

        assertThat(posting.getOriginalUrl()).isEqualTo(updatedUrl.trim());
    }
}
