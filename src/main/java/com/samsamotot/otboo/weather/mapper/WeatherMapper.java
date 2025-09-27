package com.samsamotot.otboo.weather.mapper;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.dto.*;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface WeatherMapper {

    @Mapping(target = "precipitation", expression = "java(toPrecipitationDto(weather))")
    @Mapping(target = "humidity", expression = "java(toHumidityDto(weather))")
    @Mapping(target = "temperature", expression = "java(toTemperatureDto(weather))")
    @Mapping(target = "windSpeed", expression = "java(toWindSpeedDto(weather))")
    @Mapping(target = "location", ignore = true)
    WeatherDto toDto(Weather weather);

    @Mapping(source = "weather.id", target = "id")
    @Mapping(source = "weather", target = "precipitation")
    @Mapping(source = "weather", target = "humidity")
    @Mapping(source = "weather", target = "temperature")
    @Mapping(source = "weather", target = "windSpeed")
    @Mapping(source = "location", target = "location")
    WeatherDto toDto(Weather weather, Location location, @Context Weather yesterdayWeather);

    @AfterMapping
    default void calculateComparedValues(
            @MappingTarget WeatherDto.WeatherDtoBuilder dtoBuilder,
            Weather todayWeather,
            @Context Weather yesterdayWeather
    ) {
        if (yesterdayWeather != null) {
            Double humidityCompared = todayWeather.getHumidityCurrent() - yesterdayWeather.getHumidityCurrent();
            Double temperatureCompared = todayWeather.getTemperatureCurrent() - yesterdayWeather.getTemperatureCurrent();

            dtoBuilder.humidity(new HumidityDto(todayWeather.getHumidityCurrent(), humidityCompared));
            dtoBuilder.temperature(new TemperatureDto(
                    todayWeather.getTemperatureCurrent(),
                    temperatureCompared,
                    yesterdayWeather.getTemperatureMin(),
                    yesterdayWeather.getTemperatureMax()
            ));
        }
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    default PrecipitationDto toPrecipitationDto(Weather weather) {
        if (weather == null) return null;
        return PrecipitationDto.builder()
            .type(weather.getPrecipitationType())
            .amount(weather.getPrecipitationAmount())
            .probability(weather.getPrecipitationProbability())
            .build();
    }

    default HumidityDto toHumidityDto(Weather weather) {
        if (weather == null) return null;
        return HumidityDto.builder()
            .current(weather.getHumidityCurrent())
            .comparedToDayBefore(weather.getHumidityComparedToDayBefore())
            .build();
    }

    default TemperatureDto toTemperatureDto(Weather weather) {
        if (weather == null) return null;
        return TemperatureDto.builder()
            .current(weather.getTemperatureCurrent())
            .comparedToDayBefore(weather.getTemperatureComparedToDayBefore())
            .min(weather.getTemperatureMin())
            .max(weather.getTemperatureMax())
            .build();
    }

    default WindSpeedDto toWindSpeedDto(Weather weather) {
        if (weather == null) return null;
        return WindSpeedDto.builder()
            .speed(weather.getWindSpeed())
            .asWord(weather.getWindAsWord())
            .build();
    }

    default WeatherAPILocation toWeatherAPILocation(Location location, Weather weather) {
        if (location == null || weather == null || weather.getGrid() == null) return null;

        Grid grid = weather.getGrid();

        return WeatherAPILocation.builder()
            .latitude(location.getLatitude())
            .longitude(location.getLongitude())
            .x(grid.getX())
            .y(grid.getY())
            .locationNames(location.getLocationNames())
            .build();
    }
}
