package com.chwihap.server.domain.notification.dto;

import com.chwihap.server.domain.notification.enums.NotificationStatus;
import com.chwihap.server.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 5.3 이메일 알림 발송 이력 응답 DTO
 * @param id 알림 이력 ID
 * @param type 알림 채널
 * @param cardId 연결된 칸반 카드 ID
 * @param companyName 연결된 공고의 회사명
 * @param message 발송한 알림 메시지
 * @param sentAt 이메일 발송 시도 시각
 * @param status 이메일 발송 결과
 * @author say_0
 */
@Schema(description = "이메일 알림 발송 이력")
public record NotificationHistoryItemResponse(
        @Schema(description = "알림 이력 ID", example = "1")
        Long id,

        @Schema(description = "알림 채널", example = "EMAIL", allowableValues = {"EMAIL"})
        NotificationType type,

        @Schema(description = "연결된 칸반 카드 ID", example = "10")
        Long cardId,

        @Schema(description = "연결된 공고의 회사명", example = "카카오")
        String companyName,

        @Schema(description = "발송한 알림 메시지", example = "카카오 지원 마감 D-3입니다.")
        String message,

        @Schema(description = "이메일 발송 시도 시각", example = "2026-07-12T09:00:00")
        LocalDateTime sentAt,

        @Schema(description = "이메일 발송 결과", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
        NotificationStatus status
) {
}
