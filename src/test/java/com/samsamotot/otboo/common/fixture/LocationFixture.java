package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.entity.Grid;

import java.util.List;

public class LocationFixture {
  
    public static final double DEFAULT_LATITUDE = 37.5665;
    public static final double DEFAULT_LONGITUDE = 126.9780;
    public static final int DEFAULT_X = 60;
    public static final int DEFAULT_Y = 127;
    public static final List<String> DEFAULT_LOCATIONS = List.of("서울역");

    public static Location createLocation() {
        Grid grid = new Grid(DEFAULT_X, DEFAULT_Y);
        return Location.builder()
            .latitude(DEFAULT_LATITUDE)
            .longitude(DEFAULT_LONGITUDE)
            .grid(grid)
            .locationNames(DEFAULT_LOCATIONS)
            .build();
    }

    public static Location createLocation(Grid grid) {
        return Location.builder()
            .latitude(DEFAULT_LATITUDE)
            .longitude(DEFAULT_LONGITUDE)
            .grid(grid)
            .locationNames(DEFAULT_LOCATIONS)
            .build();
    }
  
    /**
     * 유효한 위치 (갱신하지 않음) - isLocationStale()이 false를 반환
     */
    public static Location createValidLocation() {
        Grid grid = new Grid(DEFAULT_X, DEFAULT_Y);
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .grid(grid)
                .locationNames(List.of("서울특별시", "중구", "명동", ""))
                .build();
    }

    /**
     * 오래된 위치 (UNKNOWN 포함) - isLocationStale()이 true를 반환
     */
    public static Location createStaleLocation() {
        Grid grid = new Grid(0, 0);
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .grid(grid)
                .locationNames(List.of("UNKNOWN"))
                .build();
    }

    /**
     * UNKNOWN을 포함하는 위치 - isLocationStale()이 true를 반환
     */
    public static Location createLocationWithUnknown() {
        Grid grid = new Grid(DEFAULT_X, DEFAULT_Y);
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .grid(grid)
                .locationNames(List.of("서울특별시", "UNKNOWN", "명동", ""))
                .build();
    }


    /**
     * x, y가 0인 위치 - isLocationStale()이 true를 반환
     */
    public static Location createLocationWithZeroCoordinates() {
        Grid grid = new Grid(0, 0);
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .grid(grid)
                .locationNames(List.of("서울특별시", "중구", "명동", ""))
                .build();
    }

    public static Location createLocationWithCoordinates() {
        Grid grid = new Grid(999, 999);
        return Location.builder()
                .latitude(DEFAULT_LATITUDE)
                .longitude(DEFAULT_LONGITUDE)
                .grid(grid)
                .locationNames(List.of("화성", "인데경기도아님", "화성읍읍"))
                .build();
    }
}
