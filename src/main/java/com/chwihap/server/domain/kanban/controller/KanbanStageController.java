package com.chwihap.server.domain.kanban.controller;

import com.chwihap.server.domain.kanban.dto.KanbanStageRequest;
import com.chwihap.server.domain.kanban.dto.KanbanStageCreateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanStageUpdateResponse;
import com.chwihap.server.domain.kanban.service.KanbanStageService;
import com.chwihap.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kanban")
@RequiredArgsConstructor
public class KanbanStageController {

    private final KanbanStageService kanbanStageService;

    @PostMapping("/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KanbanStageCreateResponse> addToStages(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody KanbanStageRequest kanbanStageRequest
    ) {
        KanbanStageCreateResponse kanbanStageCreateResponse = kanbanStageService.addToStage(userId, kanbanStageRequest);
        return ApiResponse.success(kanbanStageCreateResponse);
    }

    @PatchMapping("/stages/{stageId}")
    public ApiResponse<KanbanStageUpdateResponse> updateStage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stageId,
            @Valid @RequestBody KanbanStageRequest kanbanStageRequest
    ) {
        KanbanStageUpdateResponse kanbanStageUpdateResponse = kanbanStageService.updateToStage(userId, stageId, kanbanStageRequest);
        return ApiResponse.success(kanbanStageUpdateResponse);
    }

    @DeleteMapping("/stages/{stageId}")
    public ApiResponse<Void> deleteStage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stageId,
            @RequestParam(required = false) Long moveToStageId
    ) {
        kanbanStageService.deleteToStage(userId, stageId, moveToStageId);
        return ApiResponse.success();
    }
}
