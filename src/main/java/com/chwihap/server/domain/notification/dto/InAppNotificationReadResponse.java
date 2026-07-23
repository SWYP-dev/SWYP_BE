package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인앱 알림 읽음 처리 응답")
public record InAppNotificationReadResponse(
        @Schema(description = "실제 읽음 처리된 인앱 알림 개수", example = "2")
        int updatedCount
) {
}
