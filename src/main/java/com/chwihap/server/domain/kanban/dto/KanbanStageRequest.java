package com.chwihap.server.domain.kanban.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record KanbanStageRequest(
        // 서비스 계층에서 이력값 검증(예외처리)
        String name,

        @NotNull @Min(1)
        Integer position
) {
        // [예외처리] 사용자 입력시 줄바꿈을 제거 후 앞뒤 공백 제거하여 저장
        public KanbanStageRequest{
                name = name == null ? null : name.replaceAll("[\\r\\n]+", "").trim();
        }
}
