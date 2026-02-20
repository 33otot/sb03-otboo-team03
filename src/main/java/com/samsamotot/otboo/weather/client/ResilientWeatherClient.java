package com.samsamotot.otboo.weather.client;

import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Primary // WeatherClient 주입 시 이 클래스가 우선순위를 가짐
@Component
public class ResilientWeatherClient implements WeatherClient {

    private final WeatherClient kmaClient;
    private final WeatherClient openWeatherMapClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    public ResilientWeatherClient(
            @Qualifier("kmaClient") WeatherClient kmaClient,
            @Qualifier("openWeatherMapClient") WeatherClient openWeatherMapClient,
            ReactiveCircuitBreakerFactory circuitBreakerFactory
    ) {
        this.kmaClient = kmaClient;
        this.openWeatherMapClient = openWeatherMapClient;
        // 'weather'라는 ID로 서킷 브레이커 인스턴스 생성 (Config 설정 적용됨)
        this.circuitBreaker = circuitBreakerFactory.create("weather");
    }

    @Override
    public Mono<WeatherForecastResponse> fetchWeather(int nx, int ny) {
        return circuitBreaker.run(
                kmaClient.fetchWeather(nx, ny), // 1. 메인(기상청) 호출 시도
                throwable -> { // 2. 실패 시 Fallback 실행
                    log.warn("⚠️ KMA API Failed! Switching to OpenWeatherMap... Error: {}", throwable.getMessage());
                    // 에러 발생 시 서브(OWM) 호출
                    return openWeatherMapClient.fetchWeather(nx, ny);
                }
        );
    }
}