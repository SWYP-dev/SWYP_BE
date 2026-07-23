package com.chwihap.server.domain.auth.controller;

import com.chwihap.server.domain.auth.dto.request.KakaoLoginRequest;
import com.chwihap.server.domain.auth.dto.request.TokenReissueRequest;
import com.chwihap.server.domain.auth.dto.response.AuthTokenResponse;
import com.chwihap.server.domain.auth.dto.response.TokenReissueResponse;
import com.chwihap.server.domain.auth.service.AuthService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        return ApiResponse.success(authService.loginWithKakao(request.code(), request.redirectUri()));
    }

    /**
     * Refresh Token으로 새 Access Token을 재발급한다.
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenReissueResponse> reissueAccessToken(@Valid @RequestBody TokenReissueRequest request) {
        return ApiResponse.success(authService.reissueAccessToken(request.refreshToken()));
    }

    /**
     * 인증된 유저의 Refresh Token을 무효화해 로그아웃 처리한다.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.id());
        return ApiResponse.success();
    }
}
