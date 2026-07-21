package com.samsamotot.otboo.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        // 최근 10개의 요청을 기준으로 판단
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        // 50% 이상 실패 시 서킷 오픈 (기상청 차단)
                        .failureRateThreshold(50)
                        // 서킷 오픈 유지 시간 (10초 후 다시 시도 - Half Open)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        // Half-Open 상태에서 허용할 요청 수
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        // 5초 동안 응답 없으면 Timeout 처리 (서브 API로 전환)
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .build());
    }
}