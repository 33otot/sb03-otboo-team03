package com.samsamotot.otboo.weather.client;

import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircuitWeatherClientTest {

    @Mock
    private WeatherClient kmaClient;

    @Mock
    private WeatherClient openWeatherMapClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private ReactiveCircuitBreakerFactory circuitBreakerFactory;

    @Mock
    private ReactiveCircuitBreaker circuitBreaker;

    private CircuitWeatherClient circuitWeatherClient;

    @BeforeEach
    void setUp() {
        // 서킷 브레이커 생성 모킹
        when(circuitBreakerFactory.create("weather")).thenReturn(circuitBreaker);
        circuitWeatherClient = new CircuitWeatherClient(kmaClient, openWeatherMapClient, circuitBreakerFactory);

        // 서킷 브레이커의 run 메서드 동작 모킹 (메인 Mono 실행 중 에러 시 Fallback 실행)
        // Java 컴파일러의 제네릭 타입 추론 오류를 방지하기 위해 any(Class) 형식으로 명시합니다.
        when(circuitBreaker.run(any(Mono.class), any(Function.class))).thenAnswer(invocation -> {
            Mono<WeatherForecastResponse> mainMono = invocation.getArgument(0);
            Function<Throwable, Mono<WeatherForecastResponse>> fallbackFunction = invocation.getArgument(1);
            return mainMono.onErrorResume(fallbackFunction);
        });
    }

    @Test
    @DisplayName("fetchWeather - 정상 동작 시 KmaClient를 호출하고 결과를 반환한다")
    void fetchWeather_Success_CallsKmaClient() {
        // given
        int nx = 60;
        int ny = 127;
        WeatherForecastResponse expectedResponse = new WeatherForecastResponse(null);

        when(kmaClient.fetchWeather(nx, ny)).thenReturn(Mono.just(expectedResponse));

        // when & then
        StepVerifier.create(circuitWeatherClient.fetchWeather(nx, ny))
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(kmaClient, times(1)).fetchWeather(nx, ny);
        verify(openWeatherMapClient, never()).fetchWeather(anyInt(), anyInt()); // 서브 API는 호출되지 않아야 함
    }

    @Test
    @DisplayName("fetchWeather - KmaClient 호출 실패 시 Fallback으로 OpenWeatherMapClient를 호출한다")
    void fetchWeather_Failure_CallsOpenWeatherMapClientFallback() {
        // given
        int nx = 60;
        int ny = 127;
        WeatherForecastResponse fallbackResponse = new WeatherForecastResponse(null);

        when(kmaClient.fetchWeather(nx, ny)).thenReturn(Mono.error(new RuntimeException("KMA API Timeout Exception")));
        when(openWeatherMapClient.fetchWeather(nx, ny)).thenReturn(Mono.just(fallbackResponse));

        // when & then
        StepVerifier.create(circuitWeatherClient.fetchWeather(nx, ny))
                .expectNext(fallbackResponse)
                .verifyComplete();

        verify(kmaClient, times(1)).fetchWeather(nx, ny); // 메인 API 실패 확인
        verify(openWeatherMapClient, times(1)).fetchWeather(nx, ny); // 서브 API 호출 확인
    }
}