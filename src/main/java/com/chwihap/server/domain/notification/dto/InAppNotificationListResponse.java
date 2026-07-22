package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 5.4 인앱 알림 목록 응답 DTO
 * @param unreadCount 읽지 않은 전체 인앱 알림 개수
 * @param items 최근 인앱 알림 목록
 * @author say_0
 */
@Schema(description = "인앱 알림 목록 응답")
public record InAppNotificationListResponse(
        @Schema(description = "읽지 않은 전체 인앱 알림 개수", example = "2")
        long unreadCount,

        @Schema(description = "최근 인앱 알림 목록")
        List<InAppNotificationItemResponse> items
) {
}
