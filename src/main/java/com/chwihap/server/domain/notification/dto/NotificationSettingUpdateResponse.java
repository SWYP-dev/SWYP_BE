package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "알림 설정 수정 응답")
public record NotificationSettingUpdateResponse(
        @Schema(description = "변경된 이메일 알림 수신 여부", example = "true")
        boolean emailEnabled,

        @Schema(description = "변경된 인앱 알림 수신 여부", example = "true")
        boolean inAppEnabled,

        @Schema(description = "서버에서 확정한 리마인드 기준일 목록. 0은 마감 당일(D-Day)", example = "[7, 3, 1, 0]")
        List<Integer> remindDays
) {
}
