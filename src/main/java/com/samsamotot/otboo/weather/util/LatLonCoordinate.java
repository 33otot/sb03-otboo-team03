package com.samsamotot.otboo.weather.util;

import lombok.Builder;

@Builder
public record LatLonCoordinate(
        double latitude, double longitude
) {
}
