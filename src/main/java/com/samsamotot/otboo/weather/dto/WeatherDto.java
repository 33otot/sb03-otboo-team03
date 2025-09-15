package com.samsamotot.otboo.weather.dto;

import com.samsamotot.otboo.weather.entity.SkyStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WeatherDto(
    UUID id,
    LocalDateTime forecastedAt,
    LocalDateTime forecastAt,
    WeatherAPILocation location,
    SkyStatus skyStatus,
    PrecipitationDto precipitation,
    HumidityDto humidity,
    TemperatureDto temperature,
    WindSpeedDto windSpeed
) {

}
