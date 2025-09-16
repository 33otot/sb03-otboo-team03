package com.samsamotot.otboo.weather.controller;

import com.samsamotot.otboo.location.entity.WeatherAPILocation;
import com.samsamotot.otboo.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 테스트용 컨트롤러 생성
@RestController
@RequestMapping("/api/weathers")
@RequiredArgsConstructor
public class WeatherController {

    private final LocationService locationService;

    @GetMapping("/location")
    public WeatherAPILocation getUserLocation(
            @RequestParam double longitude,
            @RequestParam double latitude
    ) {
        return locationService.getCurrentLocation(longitude, latitude);
    }
}
