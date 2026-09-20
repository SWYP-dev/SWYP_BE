package com.chwihap.server.domain.auth.controller;

import com.chwihap.server.domain.auth.dto.response.AuthTokenResponse;
import com.chwihap.server.domain.auth.service.AuthService;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import com.chwihap.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실사용자 UX 테스트를 위해 카카오 로그인 없이 익명 테스트 계정 세션을 발급한다.
 * app.test-session.secret이 비어있으면(기본값) 완전히 비활성화되며,
 * 값이 설정된 환경(테스트 기간 동안만 환경변수로 활성화)에서 헤더로 전달된 secret이
 * 일치할 때만 세션을 발급해 무단 계정 생성을 막는다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TestAuthController {

    private final AuthService authService;

    @Value("${app.test-session.secret:}")
    private String testSessionSecret;

    @PostMapping("/test-session")
    public ApiResponse<AuthTokenResponse> createTestSession(
            @RequestHeader(value = "X-Test-Session-Secret", required = false) String secret
    ) {
        if (!StringUtils.hasText(testSessionSecret) || !testSessionSecret.equals(secret)) {
            throw new BusinessException(ErrorCode.TEST_SESSION_NOT_ALLOWED);
        }
        return ApiResponse.success(authService.createTestSession());
    }
}
