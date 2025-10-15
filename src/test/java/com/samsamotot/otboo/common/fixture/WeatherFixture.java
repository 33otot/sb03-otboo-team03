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

    // 모든 테스트에서 일관되게 사용할 Grid 객체
    public static final Grid TEST_GRID = new Grid(60, 127);

    /**
     * 가장 기본적인 Weather 객체를 생성합니다.
     * @param forecastAt 예보 시점
     * @param forecastedAt 예보 발표 시점
     * @param temp 현재 기온
     * @param humidity 현재 습도
     * @return Weather 객체
     */
    public static Weather createWeather(Instant forecastAt, Instant forecastedAt, Double temp, Double humidity) {
        return Weather.builder()
                .grid(TEST_GRID)
                .forecastAt(forecastAt)
                .forecastedAt(forecastedAt)
                .temperatureCurrent(temp)
                .humidityCurrent(humidity)
                .build();
    }

    /**
     * 최고/최저 기온 값을 포함하는 Weather 객체를 생성합니다.
     * DB에서 값을 조회해오는 시나리오를 테스트할 때 유용합니다.
     * @param forecastAt 예보 시점
     * @param forecastedAt 예보 발표 시점
     * @param maxTemp 최고 기온
     * @param minTemp 최저 기온
     * @return Weather 객체
     */
    public static Weather createWeatherWithMaxMinTemp(Instant forecastAt, Instant forecastedAt, Double maxTemp, Double minTemp) {
        return Weather.builder()
                .grid(TEST_GRID)
                .forecastAt(forecastAt)
                .forecastedAt(forecastedAt)
                .temperatureMax(maxTemp)
                .temperatureMin(minTemp)
                .build();
    }

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
}