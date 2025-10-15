package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.common.util.CacheNames;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherDailyValueProvider {

    private static final String SERVICE_NAME = "[WeatherDailyValueProvider] ";
    private final WeatherRepository weatherRepository;

    /**
     * 특정 '날짜'의 최고/최저 기온 예보를 DB에서 조회하여 반환합니다.
     * 이 메소드의 결과는 격자 ID, 날짜, 최고/최저 여부를 기준으로 캐싱됩니다.
     */
    @Cacheable(value = CacheNames.WEATHER_DAILY, key = "{#grid.id, #date, #isMax}", unless = "#result == null")
    public Double findDailyTemperatureValue(Grid grid, LocalDate date, boolean isMax) {
        log.info(SERVICE_NAME + "캐시 없음 - DB에서 일 최고/최저 기온 조회. Grid: {}, Date: {}, isMax: {}", grid.getId(), date, isMax);
        LocalTime targetTime = isMax ? LocalTime.of(6, 0) : LocalTime.of(21, 0);
        Instant targetInstant = date.atTime(targetTime).atZone(ZoneId.of("Asia/Seoul")).toInstant();

        return weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, targetInstant)
            .map(weather -> isMax ? weather.getTemperatureMax() : weather.getTemperatureMin())
            .orElse(null);
    }
}