package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 5.1 알림 설정 조회 응답 DTO
 * @param emailEnabled 이메일 알림 수신 여부
 * @param inAppEnabled 인앱 알림 수신 여부
 * @param email 알림을 수신할 카카오 연동 이메일
 * @param remindDays 마감 리마인드 기준일 목록
 * @author say_0
 */
@Schema(description = "알림 설정 조회 응답")
public record NotificationSettingResponse(
        @Schema(description = "이메일 알림 수신 여부", example = "true")
        boolean emailEnabled,

        @Schema(description = "인앱 알림 수신 여부", example = "true")
        boolean inAppEnabled,

        @Schema(description = "알림 수신 이메일. 카카오 계정 이메일로 수정 불가", example = "user@example.com")
        String email,

        @Schema(description = "마감 리마인드 기준일 목록. 0은 마감 당일(D-Day)", example = "[7, 3, 1, 0]")
        List<Integer> remindDays
) {
}
