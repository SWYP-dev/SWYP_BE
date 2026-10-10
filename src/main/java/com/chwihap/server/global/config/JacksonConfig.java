package com.chwihap.server.global.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 관련 설정을 정의합니다.
 */
@Configuration
public class JacksonConfig {

    /**
     * 요청 필드의 "생략"과 "명시적 null"을 구분하기 위한 {@link org.openapitools.jackson.nullable.JsonNullable} 지원 모듈을 등록합니다.
     *
     * @return JsonNullableModule
     */
    @Bean
    public JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }
}
