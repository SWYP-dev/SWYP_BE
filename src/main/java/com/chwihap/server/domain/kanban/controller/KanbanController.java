package com.chwihap.server.domain.kanban.controller;

import com.chwihap.server.domain.kanban.dto.*;
import com.chwihap.server.domain.kanban.service.KanbanCardService;
import com.chwihap.server.domain.kanban.service.KanbanStageService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class KanbanController {

    private final KanbanStageService kanbanStageService;
    private final KanbanCardService kanbanCardService;

    @PostMapping("/kanban/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KanbanStageCreateResponse> addToStages(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody KanbanStageCreateRequest kanbanStageCreateRequest
    ) {
        KanbanStageCreateResponse kanbanStageCreateResponse = kanbanStageService.addToStage(principal.id(), kanbanStageCreateRequest);
        return ApiResponse.success(kanbanStageCreateResponse);
    }

    @PatchMapping("/kanban/stages/{stageId}")
    public ApiResponse<KanbanStageUpdateResponse> updateStage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long stageId,
            @Valid @RequestBody KanbanStageRequest kanbanStageRequest
    ) {
        KanbanStageUpdateResponse kanbanStageUpdateResponse = kanbanStageService.updateToStage(principal.id(), stageId, kanbanStageRequest);
        return ApiResponse.success(kanbanStageUpdateResponse);
    }

    @DeleteMapping("/kanban/stages/{stageId}")
    public ApiResponse<Void> deleteStage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long stageId,
            @RequestParam(required = false) Long moveToStageId
    ) {
        kanbanStageService.deleteToStage(principal.id(), stageId, moveToStageId);
        return ApiResponse.success();
    }

    @GetMapping("/kanban")
    public ApiResponse<KanbanBoardResponse> getKanbanBoard(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanBoardResponse response = kanbanCardService.getKanbanBoard(principal.id());
        return ApiResponse.success(response);
    }

    @PostMapping("/kanban/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KanbanCardCreateResponse> createCard(
            @Valid @RequestBody KanbanCardRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardCreateResponse response = kanbanCardService.createCard(request, principal.id());
        return ApiResponse.success(response);
    }

    @GetMapping("/kanban/cards/{cardId}")
    public ApiResponse<KanbanCardDetailResponse> getCardDetail(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardDetailResponse response = kanbanCardService.getCardDetail(principal.id(), cardId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/kanban/cards/{cardId}/stage")
    public ApiResponse<KanbanCardStageMoveResponse> moveCardStage(
            @PathVariable Long cardId,
            @Valid @RequestBody KanbanCardStageMoveRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardStageMoveResponse response = kanbanCardService.moveCardStage(principal.id(), cardId, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/kanban/cards/{cardId}/memo")
    public ApiResponse<KanbanCardMemoUpdateResponse> updateCardMemo(
            @PathVariable Long cardId,
            @Valid @RequestBody KanbanCardMemoRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardMemoUpdateResponse response = kanbanCardService.updateCardMemo(principal.id(), cardId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/kanban/cards/{cardId}")
    public ApiResponse<Void> deleteCard(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        kanbanCardService.deleteCard(principal.id(), cardId);
        return ApiResponse.success();
    }

}
