package com.samsamotot.otboo.weather.service.impl;

import com.samsamotot.otboo.weather.client.WeatherKmaClient;
import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import com.samsamotot.otboo.weather.entity.*;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import com.samsamotot.otboo.weather.service.WeatherService;
import com.samsamotot.otboo.weather.service.WeatherTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 단기예보 수집/저장 서비스 구현체.
 * <p>
 * - 기존 fetchAndSaveByLatLon 메서드를 그대로 사용하여 격자 기반 수집을 구현합니다.
 * - 남한 전역의 날씨 데이터를 주기적으로 수집하고 저장합니다.
 * </p>
 *
 * @author HuInDoL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private static final String SERVICE_NAME = "[WeatherServiceImpl] ";

    private final WeatherKmaClient weatherKmaClient;
    private final WeatherRepository weatherRepository;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private final WeatherTransactionService weatherTransactionService;

    @Async("weatherApiTaskExecutor")
    @Override
    public CompletableFuture<Void> updateWeatherDataForGrid(Grid grid) {
        return weatherKmaClient.fetchWeather(grid.getX(), grid.getY())
                .publishOn(Schedulers.boundedElastic())
                .flatMap(weatherForecastResponse -> {
                    if (!isValid(weatherForecastResponse)) {
                        log.warn(SERVICE_NAME + "API 응답 데이터가 비어있습니다. X={}, Y={}.", grid.getX(), grid.getY());
                        return Mono.empty(); // 데이터가 유효하지 않으면 여기서 체인 종료
                    }

                    List<Weather> weatherList = convertToEntities(weatherForecastResponse, grid);

                    // 3. DB 날씨 데이터 갱신
                    return Mono.fromRunnable(() ->
                            weatherTransactionService.updateWeatherData(grid, weatherList));
                })
                .doOnError(e -> log.error(SERVICE_NAME + "비동기 날씨 업데이트 작업 실패. X={}, Y={}", grid.getX(), grid.getY(), e))
                .then() // Mono<Void>로 변환
                .toFuture() // CompletableFuture<Void>로 최종 변환
                .exceptionally(e -> {
                    // doOnError에서 로그를 이미 남겼으므로, 여기서는 예외를 처리하고 null을 반환하여 CompletableFuture를 정상 완료시킵니다.
                    // 이렇게 해야 CompletableFuture.allOf() 등에서 전체 작업이 중단되지 않습니다.
                    return null;
                });
    }

    private List<Weather> convertToEntities(WeatherForecastResponse dto, Grid grid) {
        List<WeatherForecastResponse.Item> items = dto.response().body().items().item();

        // 1. '예보 대상 시간' (fcstDate + fcstTime)을 기준으로 모든 아이템 그룹화
        Map<String, List<WeatherForecastResponse.Item>> groupedByForecastTime = items.stream()
                .collect(Collectors.groupingBy(item -> item.fcstDate() + item.fcstTime()));

        // 2. 각 예보 대상 시간 그룹을 하나의 Weather 엔티티로 변환
        return groupedByForecastTime.entrySet().stream()
                .map(entry -> {
                    // 그룹의 첫 번째 아이템에서 시간 정보를 안전하게 추출합니다.
                    WeatherForecastResponse.Item firstItemInGroup = entry.getValue().get(0);

                    // '예보 대상 시각' (forecastAt) 설정: fcstDate + fcstTime
                    String fcstDateTimeStr = firstItemInGroup.fcstDate() + firstItemInGroup.fcstTime();
                    Instant forecastAt = parseToInstant(fcstDateTimeStr);

                    // '발표된 시각' (forecastedAt) 설정: baseDate + baseTime
                    String baseDateTimeStr = firstItemInGroup.baseDate() + firstItemInGroup.baseTime();
                    Instant forecastedAt = parseToInstant(baseDateTimeStr);


                    // 3-1. Weather 엔티티 생성
                    Weather.WeatherBuilder builder = Weather.builder()
                            .forecastAt(forecastAt)
                            .forecastedAt(forecastedAt)
                            .grid(grid);

                    // 3-2. 해당 시간대의 모든 카테고리(TMP, REH, SKY 등) 값을 빌더에 채워넣음
                    entry.getValue().forEach(item -> setFieldByCategory(builder, item.category(), item.fcstValue()));
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private void setFieldByCategory(Weather.WeatherBuilder builder, String category, String value) {
        try {
            switch (category) {
                /* 수치 정보 (Numeric) */
                case "POP": // 강수 확률
                    builder.precipitationProbability(Double.parseDouble(value));
                    break;
                case "REH": // 습도
                    builder.humidityCurrent(Double.parseDouble(value));
                    break;
                case "TMP": // 1시간 기온
                    builder.temperatureCurrent(Double.parseDouble(value));
                    break;
                case "TMN": // 일 최저기온
                    builder.temperatureMin(Double.parseDouble(value));
                    break;
                case "TMX": // 일 최고기온
                    builder.temperatureMax(Double.parseDouble(value));
                    break;

                /* 코드 정보 (Code -> Enum) */
                case "SKY": // 하늘 상태
                    builder.skyStatus(SkyStatus.fromCode(value));
                    break;
                case "PTY": // 강수 형태
                    builder.precipitationType(Precipitation.fromCode(value));
                    break;

                /* 특수 처리 정보 (String -> Numeric) */
                case "PCP": // 1시간 강수량 ("강수없음" 처리 포함)
                    builder.precipitationAmount(parsePrecipitationAmount(value));
                    break;

                /* 파생 정보 (Numeric -> Enum + Numeric) */
                case "WSD": // 풍속
                    double windSpeed = Double.parseDouble(value);
                    builder.windSpeed(windSpeed); // 수치 값 그대로 저장
                    builder.windAsWord(WindAsWord.fromSpeed(windSpeed)); // 정성 정보로 변환하여 저장
                    break;
            }
        } catch (Exception e) {
            // 특정 카테고리 파싱 실패 시, 로그 남기고 다음 카테고리 처리 계속 진행
            log.warn(SERVICE_NAME + "카테고리 '{}', 값 '{}' 처리 중 오류 발생: {}",
                    category, value, e.getMessage());
        }
    }

    private Instant parseToInstant(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER).atZone(ZoneId.of("Asia/Seoul")).toInstant();
    }

    private Double parsePrecipitationAmount(String value) {
        if (value == null || value.contains("없음")) return 0.0;
        return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
    }

    private boolean isValid(WeatherForecastResponse dto) {
        return dto.response() != null &&
                dto.response().body() != null &&
                dto.response().body().items() != null &&
                !dto.response().body().items().item().isEmpty();
    }


}
