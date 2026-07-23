package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "인앱 알림 목록 응답")
public record InAppNotificationListResponse(
        @Schema(description = "현재 페이지와 관계없이 로그인 사용자가 읽지 않은 전체 인앱 알림 개수", example = "15")
        long unreadCount,

        @Schema(description = "알림 ID 내림차순으로 정렬된 현재 페이지의 인앱 알림 목록")
        List<InAppNotificationItemResponse> items,

        @Schema(description = "다음 더보기 요청의 cursor에 그대로 전달할 값. 다음 페이지가 없으면 null", example = "6", nullable = true)
        String nextCursor,

        @Schema(description = "더보기로 불러올 다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
