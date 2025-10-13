package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.weather.dto.HumidityDto;
import com.samsamotot.otboo.weather.dto.PrecipitationDto;
import com.samsamotot.otboo.weather.dto.TemperatureDto;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.dto.WindSpeedDto;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.entity.WindAsWord;
import org.mapstruct.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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

    public static Weather previousWeather(Grid grid, Instant forecastedAt, Instant forecastAt) {
        return Weather.builder()
                .forecastAt(forecastAt)
                .forecastedAt(forecastedAt)
                .temperatureCurrent(DEFAULT_TEMPERATURE_CURRENT)
                .windSpeed(DEFAULT_WIND_SPEED)
                .humidityCurrent(DEFAULT_HUMIDITY_CURRENT)
                .grid(grid)
                .build();
    }

    public static Weather createWeatherWithSkyStatus(Grid grid, SkyStatus skyStatus) {
        return Weather.builder()
            .forecastAt(DEFAULT_FORECAST_AT)
            .forecastedAt(DEFAULT_FORECASTED_AT)
            .temperatureCurrent(DEFAULT_TEMPERATURE_CURRENT)
            .windSpeed(DEFAULT_WIND_SPEED)
            .humidityCurrent(DEFAULT_HUMIDITY_CURRENT)
            .skyStatus(skyStatus)
            .grid(grid)
            .build();
    }

    public static Weather createWeatherWithPrecipitation(Grid grid, Precipitation precipitationType) {
        return Weather.builder()
            .forecastAt(DEFAULT_FORECAST_AT)
            .forecastedAt(DEFAULT_FORECASTED_AT)
            .temperatureCurrent(DEFAULT_TEMPERATURE_CURRENT)
            .windSpeed(DEFAULT_WIND_SPEED)
            .humidityCurrent(DEFAULT_HUMIDITY_CURRENT)
            .precipitationType(precipitationType)
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

    public static WeatherDto createWeatherDto(Weather weather, WeatherAPILocation location) {
        return WeatherDto.builder()
                .id(UUID.randomUUID())
                .forecastedAt(LocalDateTime.parse("2025-09-26T08:00:00"))
                .forecastAt(LocalDateTime.parse("2025-09-26T11:00:00"))
                .location(WeatherAPILocation.builder()
                        .latitude(location.latitude())
                        .longitude(location.longitude())
                        .x(location.x())
                        .y(location.y())
                        .locationNames(location.locationNames())
                        .build())
                .skyStatus(SkyStatus.CLEAR)
                .precipitation(PrecipitationDto.builder()
                        .type(Precipitation.NONE)
                        .amount(0.0)
                        .probability(0.0)
                        .build())
                .humidity(HumidityDto.builder()
                        .current(20.0)
                        .comparedToDayBefore(null)
                        .build())
                .temperature(TemperatureDto.builder()
                        .current(25.0)
                        .comparedToDayBefore(null)
                        .min(15.0)
                        .max(25.0)
                        .build())
                .windSpeed(WindSpeedDto.builder()
                        .speed(3.0)
                        .asWord(WindAsWord.WEAK)
                        .build())
                .build();
    }

    public static WeatherDto createWeatherDtoWithPrecipitation(Weather weather) {
        return WeatherDto.builder()
            .forecastAt(LocalDateTime.ofInstant(weather.getForecastAt(), ZoneOffset.UTC))
            .forecastedAt(LocalDateTime.ofInstant(weather.getForecastedAt(), ZoneOffset.UTC))
            .skyStatus(weather.getSkyStatus())
            .precipitation(
                PrecipitationDto.builder()
                    .type(weather.getPrecipitationType())
                    .build()
            )
            .build();
    }
}