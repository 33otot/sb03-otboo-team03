package com.samsamotot.otboo.location.entity;

import lombok.Builder;

import java.util.List;

@Builder
public record WeatherAPILocation(
        double latitude,
        double longitude,
        Integer x,
        Integer y,
        List<String> locationNames
) {
}
