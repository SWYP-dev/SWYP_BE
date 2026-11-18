package com.chwihap.server.domain.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "의견 제출 응답")
public record FeedbackCreateResponse(
        @Schema(description = "접수된 의견 ID", example = "1")
        Long feedbackId
) {
}
