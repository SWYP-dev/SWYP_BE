package com.chwihap.server.global.config;

import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 인증 기능 머지 전까지 로컬 환경에서 API를 테스트할 수 있도록 고정 테스트 유저를 시딩한다.
 * local 프로필은 ddl-auto: create라 매 기동마다 스키마가 재생성되므로,
 * 가장 먼저 저장되는 이 유저가 app.test-user-id(기본값 1)와 항상 일치한다.
 */
@Slf4j
@Component
@Profile("local")
@Order(1)
@RequiredArgsConstructor
public class TestUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User testUser = User.create(
                "test@chwihap.com",
                "테스트유저",
                null,
                AuthProvider.KAKAO,
                "test-user"
        );
        User saved = userRepository.save(testUser);
        log.info("테스트 유저 생성됨 (id={})", saved.getId());
    }
}
