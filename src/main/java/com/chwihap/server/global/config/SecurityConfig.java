package com.chwihap.server.global.config;

import com.chwihap.server.global.auth.TestUserAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증/인가 기능은 별도 브랜치에서 구현 중이며 MVP에서 후순위로 미뤄졌다.
 * 그 사이 API 개발/테스트가 가능하도록 전체 요청을 permitAll 처리하되,
 * {@link TestUserAuthenticationFilter}가 고정 테스트 유저를 SecurityContext에 채워
 * 컨트롤러에서 @AuthenticationPrincipal로 꺼내 쓸 수 있게 한다.
 * 인증 브랜치가 머지되면 이 필터를 실제 JWT 인증 필터로 교체해야 한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final TestUserAuthenticationFilter testUserAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(testUserAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
