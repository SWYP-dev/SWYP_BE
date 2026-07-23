package com.chwihap.server.domain.auth.service;

import com.chwihap.server.domain.auth.client.KakaoOAuthClient;
import com.chwihap.server.domain.auth.client.dto.KakaoUserInfoResponse;
import com.chwihap.server.domain.auth.dto.response.AuthTokenResponse;
import com.chwihap.server.domain.auth.repository.RefreshTokenRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void 탈퇴한_회원과_동일한_카카오_계정으로_재로그인하면_신규_회원으로_가입된다() {
        String providerId = "12345";
        KakaoUserInfoResponse kakaoUserInfo = new KakaoUserInfoResponse(
                Long.valueOf(providerId),
                new KakaoUserInfoResponse.KakaoAccount(
                        "user@example.com",
                        new KakaoUserInfoResponse.Profile("닉네임", "https://image.example.com/profile.png")
                )
        );
        given(kakaoOAuthClient.getUserInfo("auth-code", "http://localhost:3000/auth/kakao/callback")).willReturn(kakaoUserInfo);
        // 탈퇴 시 providerId를 익명화하므로, deletedAt IS NULL 조건의 조회는 항상 빈 값을 반환한다.
        given(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(AuthProvider.KAKAO, providerId))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedUser, "id", 2L);
            return savedUser;
        });
        given(jwtTokenProvider.generateAccessToken(anyLong())).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(1_000L);

        AuthTokenResponse response = authService.loginWithKakao("auth-code", "http://localhost:3000/auth/kakao/callback");

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.user().id()).isEqualTo(2L);
        verify(userRepository).findByProviderAndProviderIdAndDeletedAtIsNull(AuthProvider.KAKAO, providerId);
        verify(userRepository).save(any(User.class));
    }
}
