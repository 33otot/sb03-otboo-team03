package com.samsamotot.otboo.weather.controller;

import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
public class WeatherControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // 가짜 WeatherService를 SpringContext에 등록
    private WeatherService weatherService;

    @MockitoBean
    private LocationService locationService;

    @Test
    void 위치_정보_조회_성공() throws Exception {
        // Given
        Grid grid = GridFixture.createGrid();
        Location location = LocationFixture.createLocation();
        location.setGrid(grid);

        WeatherAPILocation result = WeatherAPILocation.builder()
                .longitude(location.getLongitude())
                .latitude(location.getLatitude())
                .x(grid.getX())
                .y(grid.getY())
                .locationNames(location.getLocationNames())
                .build();

        given(locationService.getCurrentLocation(location.getLongitude(), location.getLatitude()))
                .willReturn(result);

        // When
        // Then
        mockMvc.perform(get("/api/weathers/location")
                .param("longitude", String.valueOf(location.getLongitude()))
                .param("latitude", String.valueOf(location.getLatitude())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longitude").value(result.longitude()))
                .andExpect(jsonPath("$.latitude").value(result.latitude()))
                .andExpect(jsonPath("$.x").value(result.x()))
                .andExpect(jsonPath("$.y").value(result.y()))
                .andDo(print());
    }

    @Test
    void 유효한_위치정보로_날씨_예보_조회_성공() throws Exception {
        // Given
        Grid grid = GridFixture.createGrid();
        Location location = LocationFixture.createLocation();
        location.setGrid(grid);

        WeatherAPILocation expectedLocation = WeatherAPILocation.builder()
                .longitude(location.getLongitude())
                .latitude(location.getLatitude())
                .x(grid.getX())
                .y(grid.getY())
                .locationNames(location.getLocationNames())
                .build();

        Weather weather = WeatherFixture.createWeather(grid);

        WeatherDto weatherDto = WeatherFixture.createWeatherDto(weather, expectedLocation);
        List<WeatherDto> weatherDtoList = List.of(weatherDto);

        given(weatherService.getSixDayWeather(location.getLongitude(), location.getLatitude()))
                .willReturn(weatherDtoList);

        // When
        // Then
        mockMvc.perform(get("/api/weathers")
                .param("longitude", String.valueOf(location.getLongitude()))
                .param("latitude", String.valueOf(location.getLatitude())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andDo(print());
    }
}
