package com.chwihap.server.domain.feed.sync;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 로켓펀치 Open API 호출 전용 {@link RestClient} 설정.
 * 인증은 쿼리 파라미터가 아닌 {@code X-RP-API-Key} 헤더로 전달한다.
 */
@Configuration
@EnableConfigurationProperties(RocketPunchProperties.class)
public class RocketPunchApiConfig {

    private static final String API_KEY_HEADER = "X-RP-API-Key";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient rocketPunchRestClient(RocketPunchProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(API_KEY_HEADER, properties.apiKey())
                .requestFactory(factory)
                .build();
    }
}
