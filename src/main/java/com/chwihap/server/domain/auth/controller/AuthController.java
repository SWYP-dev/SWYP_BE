package com.chwihap.server.domain.auth.controller;

import com.chwihap.server.domain.auth.dto.request.KakaoLoginRequest;
import com.chwihap.server.domain.auth.dto.response.AuthTokenResponse;
import com.chwihap.server.domain.auth.service.AuthService;
import com.chwihap.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 인가 코드를 받아 회원가입 또는 로그인을 처리하고 JWT를 발급한다.
     */
    @PostMapping("/kakao")
    public ApiResponse<AuthTokenResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        return ApiResponse.success(authService.loginWithKakao(request.code()));
    }
}
