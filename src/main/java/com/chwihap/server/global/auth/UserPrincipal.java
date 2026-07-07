package com.chwihap.server.global.auth;

/**
 * Spring Security {@code Authentication}의 principal로 사용되는 로그인 유저 정보.
 * 컨트롤러에서는 {@code @AuthenticationPrincipal UserPrincipal principal}로 주입받는다.
 * 인증 브랜치 머지 후 실제 JWT 인증 필터가 이 타입(혹은 호환되는 타입)을 SecurityContext에 채워주면,
 * 컨트롤러 코드는 변경 없이 그대로 동작한다.
 */
public record UserPrincipal(Long id) {
}
