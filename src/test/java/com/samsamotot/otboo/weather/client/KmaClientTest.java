package com.samsamotot.otboo.weather.client;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KmaClient 단위 테스트")
class KmaClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private KmaClient kmaClient;
    private final String serviceKey = "testServiceKey";

    @BeforeEach
    void setUp() {
        kmaClient = new KmaClient(webClient, serviceKey);
    }

    @Test
    @DisplayName("fetchWeather - API 정상 응답 시 WeatherForecastResponse를 성공적으로 반환한다")
    @SuppressWarnings("unchecked")
    void fetchWeather_Success() {
        // given
        int nx = 60;
        int ny = 127;
        WeatherForecastResponse expectedResponse = new WeatherForecastResponse(null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(WeatherForecastResponse.class)).thenReturn(Mono.just(expectedResponse));

        // when & then
        StepVerifier.create(kmaClient.fetchWeather(nx, ny))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    @DisplayName("fetchWeather - 5xx 에러 및 요청 실패 예외 발생 시 재시도 후 API_RETRY_FAILURE 예외를 방출한다")
    @SuppressWarnings("unchecked")
    void fetchWeather_RetryAndFail() {
        // given
        int nx = 60;
        int ny = 127;
        WebClientResponseException serverError = WebClientResponseException.create(
                500, "Internal Server Error", null, null, null
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // 5xx 서버 오류로 계속해서 예외를 방출하도록 설정
        when(responseSpec.bodyToMono(WeatherForecastResponse.class)).thenReturn(Mono.error(serverError));

        // when & then
        StepVerifier.create(kmaClient.fetchWeather(nx, ny))
                .expectError(OtbooException.class)
                .verify();
    }

    @Test
    @DisplayName("fetchWeather - 4xx 클라이언트 오류 발생 시 재시도하지 않고 예외를 즉시 방출한다")
    @SuppressWarnings("unchecked")
    void fetchWeather_ClientError_NoRetry() {
        // given
        int nx = 60;
        int ny = 127;
        WebClientResponseException clientError = WebClientResponseException.create(
                400, "Bad Request", null, null, null
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // 4xx 에러는 재시도 없이 즉각 실패해야 함
        when(responseSpec.bodyToMono(WeatherForecastResponse.class)).thenReturn(Mono.error(clientError));

        // when & then
        StepVerifier.create(kmaClient.fetchWeather(nx, ny))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    @DisplayName("fetchWeather - WebClientRequestException(타임아웃 등) 발생 시 재시도 대상이 된다")
    @SuppressWarnings("unchecked")
    void fetchWeather_RequestException_Retry() {
        // given
        int nx = 60;
        int ny = 127;
        WebClientRequestException requestException = mock(WebClientRequestException.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // 타임아웃 예외는 isRetryable 조건에 부합하므로 3회 재시도 후 OtbooException(API_RETRY_FAILURE)을 던진다.
        when(responseSpec.bodyToMono(WeatherForecastResponse.class)).thenReturn(Mono.error(requestException));

        // when & then
        StepVerifier.create(kmaClient.fetchWeather(nx, ny))
                .expectError(OtbooException.class)
                .verify();
    }
}
