package com.samsamotot.otboo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.samsamotot.otboo.common.security.jwt.TokenInvalidationService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * 테스트 환경에서 Kafka 리스너 비활성화
   */
  @Bean
  @Profile("test")
  public KafkaMessageListenerContainer<?, ?> kafkaMessageListenerContainer() {
    // 테스트 환경에서는 빈 컨테이너를 반환해 리스너 비활성화
    return null;
  }

  /**
   * 테스트 환경에서 KafkaTemplate 비활성화
   */
  @Bean
  @Profile("test")
  public KafkaTemplate<String, String> kafkaTemplate() {
    // 테스트 환경에서는 null을 반환해 Kafka 전송 비활성화
    return null;
  }

  /**
   * 테스트 환경에서 RedisConnectionFactory Mock
   */
  @Bean
  @Primary
  @Profile("test")
  public RedisConnectionFactory redisConnectionFactory() {
    return Mockito.mock(RedisConnectionFactory.class);
  }

  /**
   * 테스트 환경에서 RedisTemplate Mock
   */
  @Bean
  @Primary
  @Profile("test")
  @SuppressWarnings("unchecked")
  public RedisTemplate<String, Object> redisTemplate() {
    return Mockito.mock(RedisTemplate.class);
  }

  @Bean
  public TokenInvalidationService tokenInvalidationService() {
    return Mockito.mock(TokenInvalidationService.class);
  }

  /**
   * 테스트 환경에서 Redis Serializer Mock
   */
  @Bean("redisSerializer")
  @Profile("test")
  public GenericJackson2JsonRedisSerializer redisSerializer() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.samsamotot.otboo")
            .allowIfSubType("java.util")
            .allowIfSubType("java.time")
            .allowIfSubType("java.lang")
            .build(),
        ObjectMapper.DefaultTyping.EVERYTHING,
        JsonTypeInfo.As.PROPERTY
    );
    return new GenericJackson2JsonRedisSerializer(objectMapper);
  }

  /**
   * 테스트 환경에서 CacheManager를 NoOp으로 설정 (캐싱 비활성화)
   */
  @Bean
  @Primary
  @Profile("test")
  public CacheManager cacheManager() {
    return new NoOpCacheManager();
  }

}
