package com.chwihap.server.domain.kanban.dto;

import java.time.LocalDate;

public record KanbanCardSaveRequest(
        // 서비스 계층에서 이력값 검증(예외처리)
        String companyName,

        // 서비스 계층에서 이력값 검증(예외처리)
        String title,

        String originalUrl,

        LocalDate deadline
) {
        // [예외처리] 사용자 입력시 줄바꿈을 제거 후 앞뒤 공백 제거하여 저장
        public KanbanCardSaveRequest {
                companyName = companyName == null ? null : companyName.replaceAll("[\\r\\n]+", "").trim();
                title = title == null ? null : title.replaceAll("[\\r\\n]+", "").trim();
                originalUrl = (originalUrl == null || originalUrl.isBlank()) ? null : originalUrl.trim();
        }
}
