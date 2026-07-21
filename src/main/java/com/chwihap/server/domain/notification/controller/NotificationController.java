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
@Tag(name = "Notification", description = "알림 설정, 이메일 발송 이력, 인앱 알림 관리 API")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 5.1 알림 설정 조회<br>
     * 로그인한 사용자의 이메일/인앱 알림 수신 여부와 리마인드 기준일을 조회한다.
     * @param principal 로그인한 유저 정보
     * @return 사용자의 알림 설정 정보 반환
     * @author say_0
     */
    @GetMapping("/settings")
    @Operation(summary = "알림 설정 조회", description = "로그인한 사용자의 알림 수신 설정과 수신 이메일을 조회합니다.")
    public ApiResponse<NotificationSettingResponse> getSettings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(notificationService.getSettings(principal.id()));
    }

    /**
     * 5.2 알림 설정 수정<br>
     * 로그인한 사용자의 이메일/인앱 알림 수신 여부와 리마인드 기준일을 수정한다.
     * @param principal 로그인한 유저 정보
     * @param request 변경하려는 알림 설정 정보
     * @return 변경된 알림 설정 정보 반환
     * @author say_0
     */
    @PatchMapping("/settings")
    @Operation(summary = "알림 설정 수정", description = "이메일/인앱 알림 ON/OFF와 D-7, D-3, D-1 리마인드 기준일을 변경합니다.")
    public ApiResponse<NotificationSettingUpdateResponse> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationSettingUpdateRequest request
    ) {
        return ApiResponse.success(notificationService.updateSettings(principal.id(), request));
    }

    /**
     * 5.3 알림 발송 이력 조회<br>
     * 로그인한 사용자에게 발송된 이메일 알림 이력을 최신순으로 조회한다.
     * @param principal 로그인한 유저 정보
     * @param cursor 다음 이메일 이력을 조회하기 위한 커서
     * @param size 조회할 이메일 이력 개수
     * @return 이메일 알림 발송 이력 반환
     * @author say_0
     */
    @GetMapping("/history")
    @Operation(summary = "이메일 알림 발송 이력 조회", description = "이메일 알림의 SUCCESS/FAILED 발송 이력을 커서 기반으로 조회합니다.")
    public ApiResponse<NotificationHistoryResponse> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "다음 페이지 조회 커서(마지막 알림 ID)", example = "20")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "조회 개수(기본 20, 최대 50)", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(notificationService.getHistory(principal.id(), cursor, size));
    }

    /**
     * 5.4 인앱 알림 목록 조회<br>
     * 로그인한 사용자의 최근 인앱 알림과 읽지 않은 알림 개수를 조회한다.
     * @param principal 로그인한 유저 정보
     * @param size 조회할 인앱 알림 개수
     * @return 미읽음 개수를 포함한 인앱 알림 목록 반환
     * @author say_0
     */
    @GetMapping("/inbox")
    @Operation(summary = "인앱 알림 목록 조회", description = "최근 인앱 알림과 전체 미읽음 개수를 최신순으로 조회합니다.")
    public ApiResponse<InAppNotificationListResponse> getInbox(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "조회 개수(기본 20, 최대 50)", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(notificationService.getInbox(principal.id(), size));
    }

    /**
     * 5.5 인앱 알림 읽음 처리<br>
     * 로그인한 사용자의 인앱 알림을 단건 또는 복수 건 읽음 처리한다.
     * @param principal 로그인한 유저 정보
     * @param request 읽음 처리할 인앱 알림 ID 목록
     * @return 실제 읽음 처리된 알림 개수 반환
     * @author say_0
     */
    @PatchMapping("/inbox/read")
    @Operation(summary = "인앱 알림 읽음 처리", description = "요청한 알림 ID 중 로그인 사용자의 미읽음 인앱 알림만 읽음 처리합니다.")
    public ApiResponse<InAppNotificationReadResponse> readInbox(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InAppNotificationReadRequest request
    ) {
        return ApiResponse.success(notificationService.readInbox(principal.id(), request));
    }
}
