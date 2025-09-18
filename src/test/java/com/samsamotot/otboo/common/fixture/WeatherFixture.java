package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class WeatherFixture {

    public static final Instant DEFAULT_FORECAST_AT = Instant.now().minusSeconds(1800);
    public static final Instant DEFAULT_FORECASTED_AT = Instant.now().plusSeconds(1800);

    public static Weather createWeather(Location location) {
        return Weather.builder()
            .forecastAt(DEFAULT_FORECAST_AT)
            .forecastedAt(DEFAULT_FORECASTED_AT)
            .location(location)
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
