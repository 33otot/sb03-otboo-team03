package com.samsamotot.otboo.weather.dto;

import lombok.Builder;

@Builder
public record HumidityDto(
    Double current,
    Double comparedToDayBefore
) {

}

