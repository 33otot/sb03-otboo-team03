package com.samsamotot.otboo.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.samsamotot.otboo.common.util.CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private static final String CONFIG_NAME = "[RedisCacheConfig] ";

    /**
     * @Cacheable 어노테이션이 사용할 CacheManager를 설정하고 Bean으로 등록합니다.
     * @param redisConnectionFactory 스프링이 자동으로 주입해주는 Redis 연결 정보
     * @return 설정이 적용된 RedisCacheManager
     */
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info(CONFIG_NAME + "RedisCacheManager 초기화 시작");

        // Java 객체를 JSON으로, JSON을 Java 객체로 변환하기 위한 직렬화(Serializer) 설정
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class).build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDate, LocalDateTime 지원
        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
        GenericJackson2JsonRedisSerializer redisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 모든 캐시에 적용될 기본 정잭(Default Configuration) 정의
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
        cacheConfigurations.put(CacheNames.WEATHER, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CacheNames.PROFILE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CacheNames.WEATHER_DAILY, defaultConfig.entryTtl(Duration.ofHours(24)));


        log.info(CONFIG_NAME + "캐시 그룹별 TTL 설정: weather(1시간), profile(30분)");

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
