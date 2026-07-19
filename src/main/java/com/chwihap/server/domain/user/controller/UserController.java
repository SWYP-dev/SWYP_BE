package com.chwihap.server.domain.user.controller;

import com.chwihap.server.domain.user.service.UserService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 인증된 유저를 탈퇴 처리한다.
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal UserPrincipal principal) {
        userService.withdraw(principal.id());
        return ApiResponse.success();
    }
}
