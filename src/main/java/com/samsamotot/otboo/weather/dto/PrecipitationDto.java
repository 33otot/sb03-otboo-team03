package com.samsamotot.otboo.weather.dto;

import com.samsamotot.otboo.weather.entity.Precipitation;
import lombok.Builder;

@Builder
public record PrecipitationDto(
    Precipitation type,
    Double amount,
    Double probability
) {

}

