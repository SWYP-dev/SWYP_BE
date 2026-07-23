package com.chwihap.server.domain.notification.controller;

import com.chwihap.server.domain.notification.dto.*;
import com.chwihap.server.domain.notification.service.NotificationService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/settings")
    @Operation(
            summary = "5.1 알림 설정 조회",
            description = """
                    로그인한 사용자의 이메일/인앱 알림 수신 여부, 수신 이메일과 리마인드 기준일을 조회합니다.
                    저장된 설정이 없으면 이메일·인앱 알림 활성화 및 `[7, 3, 1, 0]` 기준으로 기본 설정을 생성합니다.
                    """
    )
    public ApiResponse<NotificationSettingResponse> getSettings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(notificationService.getSettings(principal.id()));
    }

    @PatchMapping("/settings")
    @Operation(
            summary = "5.2 알림 설정 수정",
            description = """
                    이메일/인앱 알림 ON/OFF와 리마인드 기준일을 변경합니다.
                    `remindDays`에는 `7`, `3`, `1`, `0`만 사용할 수 있으며, 중복값은 제거하고 내림차순으로 저장합니다.
                    빈 배열을 보내면 마감 리마인드를 받지 않습니다.
                    """
    )
    public ApiResponse<NotificationSettingUpdateResponse> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationSettingUpdateRequest request
    ) {
        return ApiResponse.success(notificationService.updateSettings(principal.id(), request));
    }

    @GetMapping("/history")
    @Operation(
            summary = "5.3 이메일 알림 발송 이력 조회",
            description = """
                    이메일 발송 성공·실패 이력을 최신순으로 조회합니다.

                    **커서 기반 조회 방법**
                    1. 첫 요청에서는 `cursor`를 생략합니다.
                    2. 응답의 `hasNext`가 `true`이면 `nextCursor`를 다음 요청의 `cursor`로 그대로 전달합니다.
                    3. `hasNext`가 `false`이면 마지막 페이지입니다.

                    페이지 기본 크기는 20개이며 최대 50개입니다.
                    """
    )
    public ApiResponse<NotificationHistoryResponse> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(
                    description = "첫 조회 시 생략. 다음 조회부터 직전 응답의 nextCursor를 전달",
                    example = "20"
            )
            @RequestParam(required = false) String cursor,
            @Parameter(
                    description = "페이지당 조회 개수. 생략하거나 0 이하면 20개, 50 초과이면 50개로 적용",
                    example = "20"
            )
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(notificationService.getHistory(principal.id(), cursor, size));
    }

    // TODO 더보기 구현시 Swagger 문서 수정 필요(페이지 기본 크기는 20개, 생략하거나 0 이하면 20개, example = "20")
    @GetMapping("/inbox")
    @Operation(
            summary = "5.4 인앱 알림 목록 조회(더보기)",
            description = """
                    인앱 알림을 최신순으로 조회하며, 현재 페이지와 관계없이 전체 미읽음 개수를 함께 반환합니다.

                    **더보기 사용 방법**
                    1. 첫 요청에서는 `cursor`를 생략합니다.
                    2. 응답의 `hasNext`가 `true`이면 `nextCursor`를 다음 요청의 `cursor`로 그대로 전달합니다.
                    3. 응답의 `items`를 기존 목록 뒤에 추가합니다.
                    4. `hasNext`가 `false`이면 더보기를 종료합니다.

                    페이지 기본 크기는 20개이며 최대 50개입니다.
                    """
    )
    public ApiResponse<InAppNotificationListResponse> getInbox(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(
                    description = "첫 조회 시 생략. 더보기 조회부터 직전 응답의 nextCursor를 전달",
                    example = "6"
            )
            @RequestParam(required = false) String cursor,
            @Parameter(
                    description = "페이지당 조회 개수. 생략하거나 0 이하면 20개, 50 초과이면 50개로 적용",
                    example = "20"
            )
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(notificationService.getInbox(principal.id(), cursor, size));
    }

    @PatchMapping("/inbox/read")
    @Operation(
            summary = "5.5 인앱 알림 읽음 처리",
            description = """
                    알림 ID를 한 개 이상 전달해 단건 또는 여러 건을 읽음 처리합니다.
                    로그인한 사용자의 읽지 않은 인앱 알림만 변경하며, 중복 ID와 이미 읽은 알림,
                    존재하지 않거나 다른 사용자가 소유한 알림 ID는 처리 건수에서 제외합니다.
                    """
    )
    public ApiResponse<InAppNotificationReadResponse> readInbox(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InAppNotificationReadRequest request
    ) {
        return ApiResponse.success(notificationService.readInbox(principal.id(), request));
    }
}
