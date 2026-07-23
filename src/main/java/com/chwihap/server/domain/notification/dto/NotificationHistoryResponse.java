package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "이메일 알림 발송 이력 목록 응답")
public record NotificationHistoryResponse(
        @Schema(description = "알림 ID 내림차순으로 정렬된 현재 페이지의 이메일 발송 이력")
        List<NotificationHistoryItemResponse> items,

        @Schema(description = "다음 조회 요청의 cursor에 그대로 전달할 값. 다음 페이지가 없으면 null", example = "20", nullable = true)
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext
) {
}
