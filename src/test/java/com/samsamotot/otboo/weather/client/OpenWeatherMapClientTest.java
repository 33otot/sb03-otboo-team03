package com.samsamotot.otboo.weather.client;

import com.samsamotot.otboo.weather.dto.OpenWeatherMapResponse;
import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import com.samsamotot.otboo.weather.util.GridConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenWeatherMapClient 단위 테스트")
class OpenWeatherMapClientTest {

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

    private OpenWeatherMapClient openWeatherMapClient;
    private final GridConverter gridConverter = new GridConverter();
    private final String apiKey = "testApiKey";
    private final String baseUrl = "http://api.openweathermap.org/data/2.5";

    @BeforeEach
    void setUp() {
        openWeatherMapClient = new OpenWeatherMapClient(webClient, gridConverter, apiKey, baseUrl);
    }

    @Test
    @DisplayName("fetchWeather - 정상 응답을 받았을 때 KMA 기상청 단기예보 포맷으로 정상 매핑한다")
    @SuppressWarnings("unchecked")
    void fetchWeather_Success() {
        // given
        int nx = 60;
        int ny = 127;

        OpenWeatherMapResponse.Main main = new OpenWeatherMapResponse.Main(25.5, 26.0, 20.0, 30.0, 1013, 60);
        OpenWeatherMapResponse.Wind wind = new OpenWeatherMapResponse.Wind(3.5, 180);
        OpenWeatherMapResponse.Weather weather = new OpenWeatherMapResponse.Weather(800, "Clear", "clear sky", "01d");
        OpenWeatherMapResponse owmResponse = new OpenWeatherMapResponse(
                new OpenWeatherMapResponse.Coord(126.9780, 37.5665),
                List.of(weather),
                main,
                wind,
                null,
                null,
                1620000000L,
                "Seoul"
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OpenWeatherMapResponse.class)).thenReturn(Mono.just(owmResponse));

        // when & then
        StepVerifier.create(openWeatherMapClient.fetchWeather(nx, ny))
                .expectNextMatches(response -> {
                    WeatherForecastResponse.Response rawResponse = response.response();
                    // 헤더 체크
                    if (!"00".equals(rawResponse.header().resultCode())) return false;
                    
                    // 아이템 리스트 검증 (TMP, REH, SKY, PTY 등 최소 4개 이상 매핑 체크)
                    List<WeatherForecastResponse.Item> items = rawResponse.body().items().item();
                    boolean hasTmp = items.stream().anyMatch(i -> "TMP".equals(i.category()) && "25.5".equals(i.fcstValue()));
                    boolean hasReh = items.stream().anyMatch(i -> "REH".equals(i.category()) && "60".equals(i.fcstValue()));
                    boolean hasSky = items.stream().anyMatch(i -> "SKY".equals(i.category()) && "1".equals(i.fcstValue())); // 800 -> 1(맑음)
                    boolean hasPty = items.stream().anyMatch(i -> "PTY".equals(i.category()) && "0".equals(i.fcstValue())); // Clear -> 0(없음)

                    return hasTmp && hasReh && hasSky && hasPty;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("fetchWeather - API 응답 중 필수인 main 객체가 null 일 때 RuntimeException을 던진다")
    @SuppressWarnings("unchecked")
    void fetchWeather_MainNull_ThrowsException() {
        // given
        int nx = 60;
        int ny = 127;

        // main 객체가 null 인 비정상 응답
        OpenWeatherMapResponse owmResponse = new OpenWeatherMapResponse(
                new OpenWeatherMapResponse.Coord(126.9780, 37.5665),
                null,
                null,
                null,
                null,
                null,
                1620000000L,
                "Seoul"
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OpenWeatherMapResponse.class)).thenReturn(Mono.just(owmResponse));

        // when & then
        StepVerifier.create(openWeatherMapClient.fetchWeather(nx, ny))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException 
                        && throwable.getMessage().contains("main' is null"))
                .verify();
    }
}
