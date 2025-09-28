package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.test.util.ReflectionTestUtils;

public class WeatherFixture {

    public static final Instant DEFAULT_FORECAST_AT = Instant.now().minusSeconds(1800);
    public static final Instant DEFAULT_FORECASTED_AT = Instant.now().plusSeconds(1800);
    public static final double DEFAULT_TEMPERATURE_CURRENT = 15.0;
    public static final double DEFAULT_WIND_SPEED = 2.0;
    public static final double DEFAULT_HUMIDITY_CURRENT = 50.0;

    public static Weather createWeather(Grid grid) {
        return Weather.builder()
            .forecastAt(DEFAULT_FORECAST_AT)
            .forecastedAt(DEFAULT_FORECASTED_AT)
            .temperatureCurrent(DEFAULT_TEMPERATURE_CURRENT)
            .windSpeed(DEFAULT_WIND_SPEED)
            .humidityCurrent(DEFAULT_HUMIDITY_CURRENT)
            .grid(grid)
            .build();
    }

    public static WeatherDto createWeatherDto(Weather weather) {
        return WeatherDto.builder()
            .forecastAt(LocalDateTime.ofInstant(weather.getForecastAt(), ZoneOffset.UTC))
            .forecastedAt(LocalDateTime.ofInstant(weather.getForecastedAt(), ZoneOffset.UTC))
            .skyStatus(weather.getSkyStatus())
            .build();
    }
}