package com.samsamotot.otboo.weather.controller;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.weather.entity.Grid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherController 단위 테스트")
class WeatherControllerTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private WeatherController weatherController;

    private final double VALID_LONGITUDE = 126.9780;
    private final double VALID_LATITUDE = 37.5665;

    @BeforeEach
    void setUp() {
        // 테스트 전 초기화
    }

    @Test
    @DisplayName("유효한 좌표로 요청하면 위치 정보를 반환한다")
    void 유효한_좌표로_요청하면_위치_정보를_반환() {
        // Given
        Location location = LocationFixture.createValidLocation();
        Grid grid = location.getGrid();
        WeatherAPILocation expectedLocation = WeatherAPILocation.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .x(grid.getX())
                .y(grid.getY())
                .locationNames(location.getLocationNames())
                .build();

        when(locationService.getCurrentLocation(VALID_LONGITUDE, VALID_LATITUDE))
                .thenReturn(expectedLocation);

        // When
        ResponseEntity<WeatherAPILocation> response = weatherController.getCurrentLocation(VALID_LONGITUDE, VALID_LATITUDE);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().latitude()).isEqualTo(VALID_LATITUDE);
        assertThat(response.getBody().longitude()).isEqualTo(VALID_LONGITUDE);
        assertThat(response.getBody().x()).isEqualTo(60);
        assertThat(response.getBody().y()).isEqualTo(127);
        assertThat(response.getBody().locationNames()).containsExactly("서울특별시", "중구", "명동", "");
    }

    @Test
    @DisplayName("LocationService에서 예외가 발생하면 예외가 전파된다")
    void LocationService에서_예외가_발생하면_예외가_전파() {
        // Given
        when(locationService.getCurrentLocation(VALID_LONGITUDE, VALID_LATITUDE))
                .thenThrow(new OtbooException(ErrorCode.API_CALL_ERROR));

        // When & Then
        assertThatThrownBy(() ->
            weatherController.getCurrentLocation(VALID_LONGITUDE, VALID_LATITUDE)
        )
        .isInstanceOf(OtbooException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.API_CALL_ERROR);
    }

    @Test
    @DisplayName("한국 경계 좌표로 요청하면 정상 처리된다")
    void 한국_경계_좌표로_요청하면_정상_처리() {
        // Given
        double boundaryLongitude = 125.0; // 서쪽 경계
        double boundaryLatitude = 33.2;   // 남쪽 경계

        Location location = LocationFixture.createValidLocation();
        Grid grid = location.getGrid();
        WeatherAPILocation expectedLocation = WeatherAPILocation.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .x(grid.getX())
                .y(grid.getY())
                .locationNames(location.getLocationNames())
                .build();

        when(locationService.getCurrentLocation(boundaryLongitude, boundaryLatitude))
                .thenReturn(expectedLocation);

        // When
        ResponseEntity<WeatherAPILocation> response = weatherController.getCurrentLocation(boundaryLongitude, boundaryLatitude);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("한국 최북단 좌표로 요청하면 정상 처리된다")
    void 한국_최북단_좌표로_요청하면_정상_처리() {
        // Given
        double northLongitude = 131.5; // 동쪽 경계
        double northLatitude = 38.3;   // 북쪽 경계

        Location location = LocationFixture.createValidLocation();
        Grid grid = location.getGrid();
        WeatherAPILocation expectedLocation = WeatherAPILocation.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .x(grid.getX())
                .y(grid.getY())
                .locationNames(location.getLocationNames())
                .build();

        when(locationService.getCurrentLocation(northLongitude, northLatitude))
                .thenReturn(expectedLocation);

        // When
        ResponseEntity<WeatherAPILocation> response = weatherController.getCurrentLocation(northLongitude, northLatitude);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

//    @Test
//    void 날씨를_요청하면_예보_리스트_응답() {
//        // Given
//        Grid grid = GridFixture.createGrid();
//        Location location = LocationFixture.createLocation();
//        location.setGrid(grid);
//
//        WeatherAPILocation expectedLocation = WeatherAPILocation.builder()
//                .longitude(location.getLongitude())
//                .latitude(location.getLatitude())
//                .x(grid.getX())
//                .y(grid.getY())
//                .locationNames(location.getLocationNames())
//                .build();
//
//        Weather weather = WeatherFixture.createWeather(grid);
//
//        WeatherDto weatherDto = WeatherFixture.createWeatherDto(weather, expectedLocation);
//        List<WeatherDto> weatherDtoList = List.of(weatherDto);
//
//        when(locationService.getSixDayWeather(location.getLongitude(), location.getLatitude()))
//                .thenReturn(weatherDtoList);
//    }
}

