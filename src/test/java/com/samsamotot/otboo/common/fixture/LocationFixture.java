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
  
    /**
     * 유효한 위치 (갱신하지 않음) - isLocationStale()이 false를 반환
     */
    public static Location createValidLocation() {
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .x(60)
                .y(127)
                .locationNames(List.of("서울특별시", "중구", "명동", ""))
                .build();
    }

    /**
     * 오래된 위치 (UNKNOWN 포함) - isLocationStale()이 true를 반환
     */
    public static Location createStaleLocation() {
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .x(0)
                .y(0)
                .locationNames(List.of("UNKNOWN"))
                .build();
    }

    /**
     * UNKNOWN을 포함하는 위치 - isLocationStale()이 true를 반환
     */
    public static Location createLocationWithUnknown() {
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .x(60)
                .y(127)
                .locationNames(List.of("서울특별시", "UNKNOWN", "명동", ""))
                .build();
    }


    /**
     * x, y가 0인 위치 - isLocationStale()이 true를 반환
     */
    public static Location createLocationWithZeroCoordinates() {
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .x(0)
                .y(0)
                .locationNames(List.of("서울특별시", "중구", "명동", ""))
                .build();
    }

    public static Location createLocationWithCoordinates() {
        return Location.builder()
                .latitude(DEFAULT_LATITUDE)
                .longitude(DEFAULT_LONGITUDE)
                .x(999)
                .y(999)
                .locationNames(List.of("화성", "인데경기도아님", "화성읍읍"))
                .build();
    }
}
