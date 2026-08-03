package com.chwihap.server.global.config;

import com.chwihap.server.domain.feed.dto.CachedFeedPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 공고 피드 조회 결과(feedPage) 캐싱용 RedisCacheManager 설정.
 * 배치 동기화 완료 시 @CacheEvict로 무효화되므로, TTL은 무효화 누락 등 예외 상황에 대비한 안전장치로만 둔다.
 * <p>
 * feedPage 캐시는 {@link CachedFeedPage} 한 타입만 저장하므로, 타입을 고정한
 * {@link Jackson2JsonRedisSerializer}를 쓴다. GenericJackson2JsonRedisSerializer는 커스텀
 * ObjectMapper를 넘기면 기본 타이핑(@class 메타데이터)이 자동으로 켜지지 않아, 역직렬화 시
 * LinkedHashMap으로 떨어져 캐시 히트 시 ClassCastException이 나는 문제가 있었다.
 */
@Configuration
public class RedisCacheConfig {

    private static final Duration FEED_CACHE_TTL = Duration.ofMinutes(30);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<CachedFeedPage> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, CachedFeedPage.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(FEED_CACHE_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("feedPage", defaultConfig)
                .build();
    }
}
