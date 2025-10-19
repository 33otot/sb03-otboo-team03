package com.samsamotot.otboo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
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
}
