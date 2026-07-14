package com.chwihap.server.domain.kanban.dto;


public record KanbanStageCreateRequest(
        // 서비스 계층에서 이력값 검증(예외처리)
        String name
) {
        // [예외처리] 사용자 입력시 줄바꿈을 공백으로 치환 후 앞뒤 공백 제거하여 저장
        public KanbanStageCreateRequest{
            name = name == null ? null : name.replaceAll("[\\r\\n]+", "").trim();
        }
}
