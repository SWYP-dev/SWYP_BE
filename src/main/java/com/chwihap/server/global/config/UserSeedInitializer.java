package com.chwihap.server.global.config;

import com.chwihap.server.domain.auth.entity.RefreshToken;
import com.chwihap.server.domain.auth.repository.RefreshTokenRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * 실제 카카오 로그인 없이도 API를 바로 호출해볼 수 있도록, 로컬 환경 기동 시 고정 테스트 계정을
 * 만들고(이미 있으면 재사용) 매번 새 JWT를 발급해 파일로 저장한다.
 * 배포 환경에는 절대 포함되면 안 되므로 local 프로필에서만 동작한다.
 */
@Slf4j
@Component
@Profile("local")
@Order(1)
@RequiredArgsConstructor
public class UserSeedInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private static final Path TOKEN_FILE = Path.of("local-test-tokens.log");

    @Override
    @Transactional
    public void run(String... args) {
        String content = issueTokenFor("local-dev-user", "local-dev@chwihap.local", "로컬테스트", "개인 로컬 테스트용");

        try {
            Files.writeString(TOKEN_FILE, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("테스트 계정 토큰을 {}에 저장했습니다. 터미널 자동 줄바꿈으로 토큰이 깨질 수 있으니, 콘솔 대신 이 파일에서 복사하세요.",
                    TOKEN_FILE.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException("테스트 계정 토큰 파일 저장 실패", e);
        }
    }

    private String issueTokenFor(String providerId, String email, String nickname, String label) {
        User user = userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(AuthProvider.KAKAO, providerId)
                .orElseGet(() -> userRepository.save(
                        User.create(email, nickname, null, AuthProvider.KAKAO, providerId)));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusNanos(jwtTokenProvider.getRefreshTokenExpirationMs() * 1_000_000))
                .build());

        // Swagger의 Authorize 입력창은 bearer 스킴이라 토큰값만 넣으면 "Bearer "를 자동으로 붙여서 보낸다.
        return """
                [%s] userId=%d
                accessToken: %s
                refreshToken: %s

                """.formatted(label, user.getId(), accessToken, refreshToken);
    }
}
