package com.chwihap.server.domain.auth.service;

import com.chwihap.server.domain.auth.client.KakaoOAuthClient;
import com.chwihap.server.domain.auth.client.dto.KakaoUserInfoResponse;
import com.chwihap.server.domain.auth.dto.response.AuthTokenResponse;
import com.chwihap.server.domain.auth.dto.response.TokenReissueResponse;
import com.chwihap.server.domain.auth.entity.RefreshToken;
import com.chwihap.server.domain.auth.repository.RefreshTokenRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import com.chwihap.server.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthTokenResponse loginWithKakao(String authorizationCode) {
        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthClient.getUserInfo(authorizationCode);
        String providerId = String.valueOf(kakaoUserInfo.id());

        User user = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
                .map(existingUser -> {
                    existingUser.updateProfile(kakaoUserInfo.nickname(), kakaoUserInfo.profileImageUrl());
                    return existingUser;
                })
                .orElse(null);

        boolean isNewUser = (user == null);
        if (isNewUser) {
            user = userRepository.save(User.builder()
                    .email(resolveEmail(kakaoUserInfo, providerId))
                    .nickname(kakaoUserInfo.nickname())
                    .profileImage(kakaoUserInfo.profileImageUrl())
                    .provider(AuthProvider.KAKAO)
                    .providerId(providerId)
                    .build());
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusNanos(jwtTokenProvider.getRefreshTokenExpirationMs() * 1_000_000))
                .build());

        return AuthTokenResponse.of(accessToken, refreshToken, isNewUser, user);
    }

    public TokenReissueResponse reissueAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (savedRefreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        return new TokenReissueResponse(accessToken);
    }

    /**
     * 인증된 유저의 저장된 Refresh Token을 모두 무효화한다.
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * 카카오 계정에 이메일 수집 동의를 하지 않은 경우를 대비한 임시 대체값.
     * TODO: 이메일 필수 동의 항목 지정 여부를 기획과 확정 필요.
     */
    private String resolveEmail(KakaoUserInfoResponse kakaoUserInfo, String providerId) {
        if (kakaoUserInfo.email() != null) {
            return kakaoUserInfo.email();
        }
        return "kakao_" + providerId + "@users.chwihap.com";
    }
}
