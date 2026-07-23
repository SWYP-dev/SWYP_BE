package com.chwihap.server.domain.feed.sync;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 인사혁신처 공공취업정보 API 호출 전용 {@link RestClient} 설정.
 * 응답이 XML이므로 {@link MappingJackson2XmlHttpMessageConverter}를 등록한다.
 */
@Configuration
@EnableConfigurationProperties(PersonnelJobProperties.class)
public class PersonnelJobApiConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient personnelJobRestClient(PersonnelJobProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .messageConverters(converters ->
                        converters.add(0, new MappingJackson2XmlHttpMessageConverter(new XmlMapper())))
                .build();
    }
}
