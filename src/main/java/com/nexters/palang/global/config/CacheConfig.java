package com.nexters.palang.global.config;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    // 알라딘 도서 검색(자동완성 포함) 캐시. 배치로 미리 채워두지 않고, 실제로 검색된 키워드만
    // 반응형으로 캐싱한다. TTL을 길게 잡아 같은 키워드가 여러 사용자에게서 겹칠 때 알라딘 호출을
    // 흡수하되, 너무 길면 신간이 자동완성에 늦게 반영되므로 12시간으로 제한한다.
    private static final Duration ALADIN_BOOK_SEARCH_TTL = Duration.ofHours(12);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ALADIN_BOOK_SEARCH_TTL)
                .disableCachingNullValues()
                // 캐시 값 타입(AladinSearchResult 등)을 역직렬화 시점에 정확히 복원하려면 타입 정보가
                // JSON에 함께 저장돼야 한다. 우리가 신뢰하는 내부 타입만 캐싱하므로 unsafe default
                // typing을 사용한다(사용자 입력을 그대로 역직렬화하는 경로가 아니라 안전하다).
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(GenericJacksonJsonRedisSerializer.builder()
                                .enableUnsafeDefaultTyping()
                                .build()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}
