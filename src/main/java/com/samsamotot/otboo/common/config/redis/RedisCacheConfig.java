package com.samsamotot.otboo.common.config.redis;

import com.samsamotot.otboo.common.util.CacheNames;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
@EnableCaching
@Profile("!test")
public class RedisCacheConfig {

    private static final String CONFIG_NAME = "[RedisCacheConfig] ";

    /**
     * @Cacheable 어노테이션이 사용할 CacheManager를 설정하고 Bean으로 등록합니다.
     * @param redisConnectionFactory 스프링이 자동으로 주입해주는 Redis 연결 정보
     * @return 설정이 적용된 RedisCacheManager
     */
    @Bean
    public CacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisSerializer") GenericJackson2JsonRedisSerializer redisSerializer
    ) {
        log.info(CONFIG_NAME + "RedisCacheManager 초기화 시작");

        // 모든 캐시에 적용될 기본 정책 정의
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Key는 String 형태로 저장
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Value는 위에서 설정한 JSON Serializer 사용
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                // 기본 TTL(Time-To-Live) 10분
                .entryTtl(Duration.ofMinutes(10))
                // DB 조회 결과가 null일 경우, 그 null 값을 캐싱하지 않음
                .disableCachingNullValues();

        // 특정 캐시 그룹별로 다른 TTL 적용하는 Map
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CacheNames.WEATHER_DAILY, defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put(CacheNames.PROFILE, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        log.info(CONFIG_NAME + "캐시 그룹별 TTL 설정: weather_daily(24시간), profile(30분)");

        // 최종 CacheManager
        RedisCacheManager cacheManager = RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultConfig) // 기본 정책 적용
                .withInitialCacheConfigurations(cacheConfigurations) // 특정 정책 적용
                .build();

        log.info(CONFIG_NAME + "RedisCacheManager 초기화 완료");
        return cacheManager;
    }
}
