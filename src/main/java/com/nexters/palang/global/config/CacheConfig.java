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
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
@EnableCaching
public class CacheConfig {

    // 알라딘 도서 검색(자동완성 포함) 캐시. 배치로 미리 채워두지 않고, 실제로 검색된 키워드만
    // 반응형으로 캐싱한다. TTL을 길게 잡아 같은 키워드가 여러 사용자에게서 겹칠 때 알라딘 호출을
    // 흡수하되, 너무 길면 신간이 자동완성에 늦게 반영되므로 12시간으로 제한한다.
    private static final Duration ALADIN_BOOK_SEARCH_TTL = Duration.ofHours(12);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 캐시 값 타입(AladinSearchResult 등)을 역직렬화 시점에 정확히 복원하려면 타입 정보가
        // JSON에 함께 저장돼야 한다. enableUnsafeDefaultTyping()은 클래스패스의 임의 타입을
        // 전부 역직렬화 대상으로 허용해 다형적 역직렬화 공격에 노출될 수 있으므로, 우리 패키지
        // 타입과 그 안에서 쓰는 JDK 컬렉션(List.of() 등의 내부 구현체 포함)만 허용하도록 제한한다.
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.nexters.palang")
                .allowIfSubType("java.util.")
                .build();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ALADIN_BOOK_SEARCH_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(GenericJacksonJsonRedisSerializer.builder()
                                .enableDefaultTyping(typeValidator)
                                .build()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}
