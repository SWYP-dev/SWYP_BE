package com.chwihap.server.domain.kanban.controller;

import com.chwihap.server.domain.kanban.dto.*;
import com.chwihap.server.domain.kanban.service.KanbanCardService;
import com.chwihap.server.domain.kanban.service.KanbanStageService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Kanban", description = "칸반 보드 및 지원 마감일 관리 API")
public class KanbanController {

    private final KanbanStageService kanbanStageService;
    private final KanbanCardService kanbanCardService;

    @PostMapping("/kanban/stages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "3.8 칸반 스테이지 추가",
            description = "칸반 스테이지를 추가합니다."
    )
    public ApiResponse<KanbanStageCreateResponse> addToStages(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody KanbanStageCreateRequest kanbanStageCreateRequest
    ) {
        KanbanStageCreateResponse kanbanStageCreateResponse = kanbanStageService.addStage(principal.id(), kanbanStageCreateRequest);
        return ApiResponse.success(kanbanStageCreateResponse);
    }

    @PatchMapping("/kanban/stages/{stageId}")
    @Operation(
            summary = "3.9 칸반 스테이지 수정",
            description = "스테이지 이름 또는 위치를 수정합니다."
    )
    public ApiResponse<KanbanStageUpdateResponse> updateStage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "수정할 스테이지 ID", example = "2")
            @PathVariable Long stageId,
            @Valid @RequestBody KanbanStageRequest kanbanStageRequest
    ) {
        KanbanStageUpdateResponse kanbanStageUpdateResponse = kanbanStageService.updateStage(principal.id(), stageId, kanbanStageRequest);
        return ApiResponse.success(kanbanStageUpdateResponse);
    }

    @DeleteMapping("/kanban/stages/{stageId}")
    @Operation(
            summary = "3.10 칸반 스테이지 삭제",
            description = "스테이지에 카드가 있으면 카드를 이동시킬 대상 스테이지 ID를 함께 전달합니다."
    )
    public ApiResponse<Void> deleteStage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "삭제할 스테이지 ID", example = "4")
            @PathVariable Long stageId,
            @Parameter(description = "삭제할 스테이지의 카드를 이동시킬 대상 스테이지 ID", example = "2")
            @RequestParam(required = false) Long moveToStageId
    ) {
        kanbanStageService.deleteStage(principal.id(), stageId, moveToStageId);
        return ApiResponse.success();
    }

    @GetMapping("/kanban")
    @Operation(
            summary = "3.1 칸반 보드 전체 조회",
            description = "로그인한 사용자의 스테이지와 카드를 칸반 위치 순서로 조회합니다."
    )
    public ApiResponse<KanbanBoardResponse> getKanbanBoard(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanBoardResponse response = kanbanCardService.getKanbanBoard(principal.id());
        return ApiResponse.success(response);
    }

    @GetMapping("/kanban/cards/deadlines")
    @Operation(
            summary = "3.12 지원 마감일 목록 조회",
            description = """
                    로그인한 사용자의 모든 칸반 스테이지에 등록된 공고를 지원 마감일 오름차순으로 조회합니다.
                    서버 기본 시간대의 오늘 날짜를 기준으로 오늘 마감인 공고는 포함하고,
                    마감일이 지났거나 마감일이 없는 공고는 제외합니다.
                    목록의 `cardId`로 카드 상세 조회 API를 호출해 드로어 상세 정보를 조회할 수 있습니다.
                    """
    )
    public ApiResponse<KanbanDeadlineListResponse> getDeadlineCards(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanDeadlineListResponse response = kanbanCardService.getDeadlineCards(principal.id());
        return ApiResponse.success(response);
    }

    @PostMapping("/kanban/cards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "3.2 칸반 카드 등록",
            description = "사용자에게 저장된 공고를 기본 스테이지에 칸반 카드로 등록합니다."
    )
    public ApiResponse<KanbanCardCreateResponse> createCard(
            @Valid @RequestBody KanbanCardRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardCreateResponse response = kanbanCardService.createCard(request, principal.id());
        return ApiResponse.success(response);
    }

    @PostMapping("/kanban/cards/direct")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "3.3 칸반 카드 직접 등록",
            description = "사용자가 직접 입력한 공고 정보로 칸반 카드를 등록합니다."
    )
    public ApiResponse<KanbanCardCreateResponse> createDirectCard(
            @Valid @RequestBody KanbanCardSaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardCreateResponse response = kanbanCardService.createDirectCard(request, principal.id());
        return ApiResponse.success(response);
    }

    @GetMapping("/kanban/cards/{cardId}")
    @Operation(
            summary = "3.5 칸반 카드 상세 조회",
            description = "기업명, 공고명, 직무 분류, 마감일, 지역, 경력, 원본 공고 URL, 메모 및 서류를 조회합니다."
    )
    public ApiResponse<KanbanCardDetailResponse> getCardDetail(
            @Parameter(description = "조회할 카드 ID", example = "10")
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardDetailResponse response = kanbanCardService.getCardDetail(principal.id(), cardId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/kanban/cards/{cardId}/stage")
    @Operation(
            summary = "3.11 칸반 카드 스테이지 이동",
            description = "카드를 지정한 스테이지와 위치로 이동합니다."
    )
    public ApiResponse<KanbanCardStageMoveResponse> moveCardStage(
            @Parameter(description = "이동할 카드 ID", example = "10")
            @PathVariable Long cardId,
            @Valid @RequestBody KanbanCardStageMoveRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardStageMoveResponse response = kanbanCardService.moveCardStage(principal.id(), cardId, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/kanban/cards/{cardId}/update")
    @Operation(
            summary = "3.4 칸반 카드 공고 정보 수정",
            description = "사용자가 직접 등록한 DIRECT 카드의 기업명, 공고명, 원본 URL 및 마감일을 수정합니다."
    )
    public ApiResponse<KanbanCardCreateResponse> updateCard(
            @Parameter(description = "수정할 카드 ID", example = "10")
            @PathVariable Long cardId,
            @Valid @RequestBody KanbanCardSaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardCreateResponse response = kanbanCardService.updateCard(principal.id(), cardId, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/kanban/cards/{cardId}/memo")
    @Operation(
            summary = "3.6 칸반 카드 메모 수정",
            description = "칸반 카드 메모를 수정한다."
    )
    public ApiResponse<KanbanCardMemoUpdateResponse> updateCardMemo(
            @Parameter(description = "메모를 수정할 카드 ID", example = "10")
            @PathVariable Long cardId,
            @Valid @RequestBody KanbanCardMemoRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        KanbanCardMemoUpdateResponse response = kanbanCardService.updateCardMemo(principal.id(), cardId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/kanban/cards/{cardId}")
    @Operation(
            summary = "3.7 칸반 카드 삭제",
            description = "칸반 카드를 삭제한다."
    )
    public ApiResponse<Void> deleteCard(
            @Parameter(description = "삭제할 카드 ID", example = "10")
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        kanbanCardService.deleteCard(principal.id(), cardId);
        return ApiResponse.success();
    }

}
