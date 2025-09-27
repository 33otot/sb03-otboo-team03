package com.samsamotot.otboo.weather.mapper;

import com.samsamotot.otboo.weather.dto.HumidityDto;
import com.samsamotot.otboo.weather.dto.PrecipitationDto;
import com.samsamotot.otboo.weather.dto.TemperatureDto;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.dto.WindSpeedDto;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeatherMapper {

    @Mapping(target = "precipitation", expression = "java(toPrecipitationDto(weather))")
    @Mapping(target = "humidity", expression = "java(toHumidityDto(weather))")
    @Mapping(target = "temperature", expression = "java(toTemperatureDto(weather))")
    @Mapping(target = "windSpeed", expression = "java(toWindSpeedDto(weather))")
    @Mapping(target = "location", ignore = true)
    WeatherDto toDto(Weather weather);

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
}
