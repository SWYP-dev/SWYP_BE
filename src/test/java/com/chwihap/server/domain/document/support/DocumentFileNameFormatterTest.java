package com.chwihap.server.domain.document.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFileNameFormatterTest {

    @Test
    void 첫_파일은_원본_이름을_유지한다() {
        assertThat(DocumentFileNameFormatter.format("파일.pdf", 0))
                .isEqualTo("파일.pdf");
    }

    @Test
    void 두_번째_파일부터_확장자_앞에_버전을_표시한다() {
        assertThat(DocumentFileNameFormatter.format("파일.pdf", 1))
                .isEqualTo("파일_v1.pdf");
        assertThat(DocumentFileNameFormatter.format("파일.pdf", 2))
                .isEqualTo("파일_v2.pdf");
    }
}
