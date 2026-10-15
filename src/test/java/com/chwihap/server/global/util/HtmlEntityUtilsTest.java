package com.chwihap.server.global.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlEntityUtilsTest {

    @Test
    void 이름_엔티티와_숫자_엔티티를_원래_문자로_디코딩한다() {
        String decoded = HtmlEntityUtils.decode("&apos;26 신입 &#39;공채&#39; &quot;R&amp;D&quot; &lt;정규직&gt;");

        assertThat(decoded).isEqualTo("'26 신입 '공채' \"R&D\" <정규직>");
    }

    @Test
    void 이중으로_escape된_엔티티까지_디코딩한다() {
        String decoded = HtmlEntityUtils.decode("&amp;apos;26 하반기 채용");

        assertThat(decoded).isEqualTo("'26 하반기 채용");
    }

    @Test
    void 엔티티가_없는_문자열은_그대로_반환한다() {
        String decoded = HtmlEntityUtils.decode("2026년 하반기 R&D 신입 채용");

        assertThat(decoded).isEqualTo("2026년 하반기 R&D 신입 채용");
    }

    @Test
    void 유효하지_않은_숫자_엔티티가_있어도_예외_없이_원래_값을_반환한다() {
        String decoded = HtmlEntityUtils.decode("공고&#-1;제목");

        assertThat(decoded).isEqualTo("공고&#-1;제목");
    }

    @Test
    void null이면_null을_반환한다() {
        assertThat(HtmlEntityUtils.decode(null)).isNull();
    }
}
