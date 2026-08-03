package com.chwihap.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 5.4 인앱 알림 항목 응답 DTO
 * @param id 인앱 알림 ID
 * @param cardId 연결된 칸반 카드 ID
 * @param dDayLabel 화면에 표시할 D-Day 배지 문구
 * @param title 인앱 알림 제목
 * @param description 인앱 알림 상세 문구
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

        @Schema(description = "화면에 표시할 마감 배지", example = "D-1")
        String dDayLabel,

        @Schema(description = "인앱 알림 제목", example = "와탭랩스 공고가 하루 남았어요 ⏰")
        String title,

        @Schema(description = "인앱 알림 상세 문구", example = "Java/Spring Boot 백엔드 개발자 채용 · 아직 지원 전이라면 서둘러 주세요!")
        String description,

        @Schema(description = "인앱 알림 읽음 여부", example = "false")
        boolean isRead,

        @Schema(description = "인앱 알림 생성 시각", example = "2026-07-12T09:00:00")
        LocalDateTime createdAt
) {
}
