package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 5.5 인앱 알림 읽음 처리 요청 DTO
 * @param ids 읽음 처리할 인앱 알림 ID 목록
 * @author say_0
 */
@Schema(description = "인앱 알림 읽음 처리 요청")
public record InAppNotificationReadRequest(
        @Schema(description = "읽음 처리할 인앱 알림 ID 목록", example = "[4, 5]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty List<@NotNull Long> ids
) {
}
