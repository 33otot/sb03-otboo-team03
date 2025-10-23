package com.samsamotot.otboo.weather.dto;

import lombok.Builder;

@Builder
public record TemperatureDto(
    Double current,
    Double comparedToDayBefore,
    Double min,
    Double max
) {

}
