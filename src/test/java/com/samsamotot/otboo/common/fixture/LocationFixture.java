package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.location.entity.Location;

import java.util.List;

public class LocationFixture {

    public static Location createLocation() {
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .x(60)
                .y(127)
                .locationNames(List.of("서울특별시", "중구", "명동", ""))
                .build();
    }

    public static Location createLocationWithCoordinates(double latitude, double longitude) {
        return Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .x(0)
                .y(0)
                .locationNames(List.of("UNKNOWN"))
                .build();
    }
}
