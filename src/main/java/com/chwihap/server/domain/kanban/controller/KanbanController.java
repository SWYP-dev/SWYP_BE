package com.chwihap.server.domain.kanban.controller;

import com.chwihap.server.domain.kanban.dto.KanbanRequest;
import com.chwihap.server.domain.kanban.dto.KanbanResponse;
import com.chwihap.server.domain.kanban.service.KanbanService;
import com.chwihap.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kanban")
@RequiredArgsConstructor
public class KanbanController {

    private final KanbanService kanbanService;

    @PostMapping("/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KanbanResponse> addToStages(
            @AuthenticationPrincipal Long userId,
            @RequestBody KanbanRequest kanbanRequest
    ) {
        KanbanResponse kanbanResponse = kanbanService.addToStage(userId, kanbanRequest);
        return ApiResponse.success(kanbanResponse);
    }
}
