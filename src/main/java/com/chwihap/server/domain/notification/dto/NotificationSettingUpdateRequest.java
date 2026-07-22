package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 5.2 알림 설정 수정 요청 DTO
 * @param emailEnabled 이메일 알림 수신 여부
 * @param inAppEnabled 인앱 알림 수신 여부
 * @param remindDays 마감 리마인드 기준일 목록
 * @author say_0
 */
@Schema(description = "알림 설정 수정 요청")
public record NotificationSettingUpdateRequest(
        @Schema(description = "이메일 알림 수신 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean emailEnabled,

        @Schema(description = "인앱 알림 수신 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean inAppEnabled,

        @Schema(description = "마감 리마인드 기준일 목록. 7, 3, 1, 0만 선택 가능(0은 마감 당일 D-Day)", example = "[7, 3, 1, 0]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull List<Integer> remindDays
) {
}
