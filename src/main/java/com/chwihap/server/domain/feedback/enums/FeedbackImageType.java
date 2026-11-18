package com.chwihap.server.domain.feedback.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 의견 첨부 이미지 형식. 파일 내용(매직바이트)으로 판정한 결과이며,
 * S3 저장 시 Content-Type과 key 확장자로 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum FeedbackImageType {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg");

    private final String contentType;
    private final String extension;
}
