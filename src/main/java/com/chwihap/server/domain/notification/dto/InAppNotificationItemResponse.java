package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 5.4 인앱 알림 항목 응답 DTO
 * @param id 인앱 알림 ID
 * @param cardId 연결된 칸반 카드 ID
 * @param companyName 연결된 공고의 회사명
 * @param message 인앱 알림 메시지
 * @param isRead 읽음 여부
 * @param createdAt 인앱 알림 생성 시각
 * @author say_0
 */
@Schema(description = "인앱 알림 항목")
public record InAppNotificationItemResponse(
        @Schema(description = "인앱 알림 ID", example = "5")
        Long id,

        @Schema(description = "연결된 칸반 카드 ID", example = "10")
        Long cardId,

        @Schema(description = "연결된 공고의 회사명", example = "카카오")
        String companyName,

        @Schema(description = "인앱 알림 메시지", example = "카카오 지원 마감 D-3입니다.")
        String message,

        @Schema(description = "인앱 알림 읽음 여부", example = "false")
        boolean isRead,

        @Schema(description = "인앱 알림 생성 시각", example = "2026-07-12T09:00:00")
        LocalDateTime createdAt
) {
}
