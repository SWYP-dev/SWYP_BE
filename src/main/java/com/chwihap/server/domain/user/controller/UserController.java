package com.chwihap.server.domain.user.controller;

import com.chwihap.server.domain.user.dto.UserMeResponse;
import com.chwihap.server.domain.user.service.UserService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "내 정보 조회",
            description = """
                    로그인한 사용자의 계정 이메일을 조회합니다.
                    `email`은 연락 가능한 실제 이메일일 때만 반환하며, 카카오 이메일 미동의 등으로 서비스가 부여한 대체 이메일이면 `null`입니다.
                    의견 보내기의 '서비스 이용 문의' 이메일 자동 입력에 사용합니다.
                    """
    )
    public ApiResponse<UserMeResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.getMe(principal.id()));
    }

    /**
     * 인증된 유저를 탈퇴 처리한다.
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal UserPrincipal principal) {
        userService.withdraw(principal.id());
        return ApiResponse.success();
    }
}
