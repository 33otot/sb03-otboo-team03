package com.samsamotot.otboo.weather.controller;

import com.samsamotot.otboo.location.entity.WeatherAPILocation;
import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.weather.controller.api.WeatherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * 날씨 관련 API를 제공하는 컨트롤러
 * 
 * <p>위치 기반 날씨 정보 조회를 위한 REST API 엔드포인트를 제공합니다.
 * 좌표를 기반으로 현재 위치 정보를 조회하고, 카카오 로컬 API를 통해
 * 행정구역 정보를 포함한 위치 데이터를 반환합니다.</p>
 * 
 * @author HuInDoL
 * @since 1.0
 * @see LocationService
 * @see WeatherAPILocation
 */
@RestController
@RequestMapping("/api/weathers")
@RequiredArgsConstructor
public class WeatherController implements WeatherApi {

    private final LocationService locationService;

    /**
     * 좌표를 기반으로 현재 위치 정보를 조회합니다.
     * 
     * <p>이 API는 다음과 같은 기능을 제공합니다:</p>
     * <ul>
     *   <li>WGS84 좌표계의 경도/위도를 받아 위치 정보 조회</li>
     *   <li>기존 위치 정보가 있으면 캐시된 데이터 반환</li>
     *   <li>새로운 위치면 카카오 로컬 API를 통해 주소 정보 조회</li>
     *   <li>행정구역 정보(시/도, 시/군/구, 읍/면/동, 리/동) 포함</li>
     * </ul>
     * 
     * <p>응답 데이터:</p>
     * <ul>
     *   <li>latitude: 위도</li>
     *   <li>longitude: 경도</li>
     *   <li>x, y: 카카오맵 좌표</li>
     *   <li>locationNames: 행정구역명 배열 [시/도, 시/군/구, 읍/면/동, 리/동]</li>
     * </ul>
     * 
     * @param longitude 경도 (WGS84 좌표계, 124.5 ~ 131.9, 한국 범위)
     * @param latitude  위도 (WGS84 좌표계, 33.0 ~ 38.6, 한국 범위)
     * @return WeatherAPILocation 위치 정보 객체
     * 
     * @since 1.0
     */
    @Override
    public ResponseEntity<WeatherAPILocation> getCurrentLocation(
            @RequestParam @Valid @DecimalMin(value = "125.0", message = "경도는 125.0 이상이어야 합니다 (한국 육지 서쪽)")
            @DecimalMax(value = "131.5", message = "경도는 131.5 이하여야 합니다 (한국 육지 동쪽)") double longitude,
            @RequestParam @Valid @DecimalMin(value = "33.2", message = "위도는 33.2 이상이어야 합니다 (제주도 육지)")
            @DecimalMax(value = "38.3", message = "위도는 38.3 이하여야 합니다 (한국 육지 북쪽)") double latitude
    ) {
        WeatherAPILocation location = locationService.getCurrentLocation(longitude, latitude);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(location);
    }
}
