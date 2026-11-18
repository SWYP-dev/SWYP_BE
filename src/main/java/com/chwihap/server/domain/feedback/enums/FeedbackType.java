package com.chwihap.server.domain.feedback.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문의 유형. BUG_REPORT: 오류 제보, FEATURE_SUGGESTION: 기능 개선 제안, SERVICE_INQUIRY: 서비스 이용 문의, ETC: 기타")
public enum FeedbackType {
    BUG_REPORT,
    FEATURE_SUGGESTION,
    SERVICE_INQUIRY,
    ETC
}
