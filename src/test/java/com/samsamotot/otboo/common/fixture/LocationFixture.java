package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.location.entity.Location;
import java.util.List;

public class LocationFixture {

    public static final double DEFAULT_LATITUDE = 37.5665;
    public static final double DEFAULT_LONGITUDE = 126.9780;
    public static final int DEFAULT_X = 60;
    public static final int DEFAULT_Y = 127;
    public static final List<String> DEFAULT_LOCATIONS = List.of("서울역");

    public static Location createLocation() {
        return Location.builder()
            .latitude(DEFAULT_LATITUDE)
            .longitude(DEFAULT_LONGITUDE)
            .x(DEFAULT_X)
            .y(DEFAULT_Y)
            .locationNames(DEFAULT_LOCATIONS)
            .build();
    }
}
