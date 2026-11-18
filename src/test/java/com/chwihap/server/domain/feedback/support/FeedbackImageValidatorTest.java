package com.chwihap.server.domain.feedback.support;

import com.chwihap.server.domain.feedback.enums.FeedbackImageType;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackImageValidatorTest {

    private static final byte[] PNG_HEADER = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] GIF_HEADER = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final int TEN_MB = 10 * 1024 * 1024;

    private final FeedbackImageValidator validator = new FeedbackImageValidator();

    @Test
    void PNG_파일은_PNG_타입으로_판정한다() {
        assertThat(validator.validate(image("error.png", PNG_HEADER))).isEqualTo(FeedbackImageType.PNG);
    }

    @Test
    void 대문자_JPG_확장자도_허용한다() {
        assertThat(validator.validate(image("ERROR.JPG", JPEG_HEADER))).isEqualTo(FeedbackImageType.JPEG);
    }

    @Test
    void JPEG_확장자를_허용한다() {
        assertThat(validator.validate(image("error.jpeg", JPEG_HEADER))).isEqualTo(FeedbackImageType.JPEG);
    }

    @Test
    void GIF_파일은_거부한다() {
        assertRejected(image("error.gif", GIF_HEADER), ErrorCode.FEEDBACK_IMAGE_UNSUPPORTED_TYPE);
    }

    @Test
    void 확장자가_png여도_내용이_GIF면_거부한다() {
        assertRejected(image("error.png", GIF_HEADER), ErrorCode.FEEDBACK_IMAGE_UNSUPPORTED_TYPE);
    }

    @Test
    void 확장자가_png인데_내용이_JPEG면_거부한다() {
        assertRejected(image("error.png", JPEG_HEADER), ErrorCode.FEEDBACK_IMAGE_UNSUPPORTED_TYPE);
    }

    @Test
    void 확장자가_없으면_거부한다() {
        assertRejected(image("error", PNG_HEADER), ErrorCode.FEEDBACK_IMAGE_UNSUPPORTED_TYPE);
    }

    @Test
    void 내용이_매직바이트보다_짧으면_거부한다() {
        assertRejected(image("error.png", new byte[]{(byte) 0x89, 0x50}), ErrorCode.FEEDBACK_IMAGE_UNSUPPORTED_TYPE);
    }

    @Test
    void 정확히_10MB면_허용한다() {
        assertThat(validator.validate(image("error.png", sized(TEN_MB)))).isEqualTo(FeedbackImageType.PNG);
    }

    @Test
    void 크기가_10MB를_1바이트_넘으면_거부한다() {
        assertRejected(image("error.png", sized(TEN_MB + 1)), ErrorCode.FEEDBACK_IMAGE_SIZE_EXCEEDED);
    }

    private MockMultipartFile image(String fileName, byte[] content) {
        return new MockMultipartFile("image", fileName, "image/png", content);
    }

    private byte[] sized(int size) {
        byte[] content = new byte[size];
        System.arraycopy(PNG_HEADER, 0, content, 0, PNG_HEADER.length);
        return content;
    }

    private void assertRejected(MockMultipartFile file, ErrorCode errorCode) {
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
