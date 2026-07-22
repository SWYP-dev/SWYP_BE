package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 5.3 이메일 알림 발송 이력 목록 응답 DTO
 * @param items 이메일 알림 발송 이력 목록
 * @param nextCursor 다음 페이지 조회 커서
 * @param hasNext 다음 페이지 존재 여부
 * @author say_0
 */
@Schema(description = "이메일 알림 발송 이력 목록 응답")
public record NotificationHistoryResponse(
        @Schema(description = "이메일 알림 발송 이력 목록")
        List<NotificationHistoryItemResponse> items,

        @Schema(description = "다음 페이지 조회 커서. 다음 페이지가 없으면 null", example = "20", nullable = true)
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext
) {
}
