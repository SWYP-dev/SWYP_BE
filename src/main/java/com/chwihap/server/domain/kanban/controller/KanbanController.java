package com.chwihap.server.domain.kanban.controller;

import com.chwihap.server.domain.kanban.dto.*;
import com.chwihap.server.domain.kanban.service.KanbanCardService;
import com.chwihap.server.domain.kanban.service.KanbanStageService;
import com.chwihap.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class KanbanController {

    private final KanbanStageService kanbanStageService;
    private final KanbanCardService kanbanCardService;

    @PostMapping("/kanban/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KanbanStageCreateResponse> addToStages(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody KanbanStageRequest kanbanStageRequest
    ) {
        KanbanStageCreateResponse kanbanStageCreateResponse = kanbanStageService.addToStage(userId, kanbanStageRequest);
        return ApiResponse.success(kanbanStageCreateResponse);
    }

    @PatchMapping("/kanban/stages/{stageId}")
    public ApiResponse<KanbanStageUpdateResponse> updateStage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stageId,
            @Valid @RequestBody KanbanStageRequest kanbanStageRequest
    ) {
        KanbanStageUpdateResponse kanbanStageUpdateResponse = kanbanStageService.updateToStage(userId, stageId, kanbanStageRequest);
        return ApiResponse.success(kanbanStageUpdateResponse);
    }

    @DeleteMapping("/kanban/stages/{stageId}")
    public ApiResponse<Void> deleteStage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stageId,
            @RequestParam(required = false) Long moveToStageId
    ) {
        kanbanStageService.deleteToStage(userId, stageId, moveToStageId);
        return ApiResponse.success();
    }

    @GetMapping("/kanban")
    public ApiResponse<KanbanBoardResponse> getKanbanBoard(
            @AuthenticationPrincipal Long userId
    ) {
        KanbanBoardResponse response = kanbanCardService.getKanbanBoard(1L);
        return ApiResponse.success(response);
    }

    @PostMapping("/kanban/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KanbanCardCreateResponse> createCard(
            @Valid @RequestBody KanbanCardRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        KanbanCardCreateResponse response = kanbanCardService.createCard(request, 1L);
        return ApiResponse.success(response);
    }

}
