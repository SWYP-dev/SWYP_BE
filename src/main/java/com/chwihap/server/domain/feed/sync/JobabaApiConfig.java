package com.chwihap.server.domain.feed.sync;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * 잡아바 채용정보 API 호출 전용 {@link RestClient} 설정.
 * <p>
 * 이 API는 두 가지 특이한 점이 있어 기본 설정만으로는 호출이 실패한다.
 * <ol>
 *   <li>User-Agent 헤더가 없는 요청은 WAF(보안 정책)에 의해 차단되고 안내용 HTML이 내려온다.
 *       브라우저 User-Agent를 기본 헤더로 붙여 우회한다.</li>
 *   <li>{@code Type=json} 요청에도 응답 {@code Content-Type}이 {@code text/html;charset=UTF-8}로
 *       잘못 내려온다(실제 바디는 JSON). 기본 Jackson 컨버터는 {@code application/json}만 지원해서
 *       매칭에 실패하므로, {@code text/html}도 지원 미디어타입에 추가한 컨버터를 등록한다.</li>
 * </ol>
 */
@Configuration
@EnableConfigurationProperties(JobabaProperties.class)
public class JobabaApiConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    @Bean
    public RestClient jobabaRestClient(JobabaProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("User-Agent", BROWSER_USER_AGENT)
                .messageConverters(converters -> converters.add(0, jsonConverter))
                .build();
    }
}
