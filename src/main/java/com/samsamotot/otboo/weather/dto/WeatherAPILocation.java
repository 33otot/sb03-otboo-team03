package com.samsamotot.otboo.weather.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record WeatherAPILocation(
    Double latitude,
    Double longitude,
    int x,
    int y,
    List<String> locationNames
) {

}
