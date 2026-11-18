package com.chwihap.server.domain.feedback.dto;

import com.chwihap.server.domain.feedback.enums.FeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "의견 제출 요청 (multipart/form-data 필드)")
public record FeedbackCreateRequest(
        @Schema(description = "문의 유형 (필수)", example = "SERVICE_INQUIRY", requiredMode = Schema.RequiredMode.REQUIRED)
        FeedbackType type,

        @Schema(description = "문의 내용 (필수, 앞뒤 공백 제외 최대 1,000자)", example = "알림 메일이 오지 않아요.", requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @Schema(description = "답변 받을 이메일. SERVICE_INQUIRY일 때만 필수이며, 다른 유형이면 저장하지 않음", example = "user@example.com", nullable = true)
        String email
) {
}
