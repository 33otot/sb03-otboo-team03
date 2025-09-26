package com.samsamotot.otboo.weather.controller;

import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.weather.controller.api.WeatherApi;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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
@Slf4j
@RestController
@RequestMapping("/api/weathers")
@RequiredArgsConstructor
public class WeatherController implements WeatherApi {

    private static final String CONTROLLER_NAME = "[WeatherController] ";

    private final LocationService locationService;
    private final WeatherService weatherService;

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
    @GetMapping("/location")
    public ResponseEntity<WeatherAPILocation> getCurrentLocation(
            @RequestParam double longitude,
            @RequestParam double latitude
    ) {
        log.info(CONTROLLER_NAME + "현재 위치 정보 조회: longitude={}, latitude={}", longitude, latitude);
        WeatherAPILocation location = locationService.getCurrentLocation(longitude, latitude);

        log.info(CONTROLLER_NAME + "위치 정보 조회 완료: longitude={}, latutude={}, 행정구역명= {}",
                longitude, latitude, location.locationNames());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(location);
    }

    @GetMapping
    public ResponseEntity<List<WeatherDto>> getSixDayWeather(
            @RequestParam double longitude,
            @RequestParam double latitude
    ) {
        log.info(CONTROLLER_NAME + "날씨 데이터 조회: longitude={}, latutude={}", longitude, latitude);

        List<WeatherDto> weathers = weatherService.getSixDayWeather(longitude, latitude);

        log.info(CONTROLLER_NAME + "날씨 데이터 조회 완료: longitude={}, latutude={}", longitude, latitude);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(weathers);
    }
}
