package com.samsamotot.otboo.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito를 사용하기 위한 확장
class WeatherDailyValueProviderTest {

    @InjectMocks // 테스트 대상 클래스. @Mock으로 생성된 객체들이 주입됩니다.
    private WeatherDailyValueProvider weatherDailyValueProvider;

    @Mock // 의존성을 가진 가짜(Mock) 객체
    private WeatherRepository weatherRepository;

    private Grid grid;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        // 테스트에 사용할 Mock 객체 설정
        grid = GridFixture.createGrid(); // 실제 객체 대신 간단히 생성
        date = LocalDate.of(2024, 7, 25);
    }

    @Test
    @DisplayName("최고 기온 조회 성공")
    void findDailyTemperatureValue_Max_Success() {
        // given
        boolean isMax = true;
        double expectedMaxTemp = 29.5;
        Weather mockWeather = Weather.builder()
                .temperatureMax(expectedMaxTemp)
                .build();

        // 어떤 Instant 값이든 findTopBy... 메소드가 호출되면 mockWeather를 반환하도록 설정
        // 단위 테스트에서는 정확한 시간 값보다 로직의 흐름이 더 중요합니다.
        given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(any(Grid.class), any(Instant.class)))
                .willReturn(Optional.of(mockWeather));

        // when
        Double result = weatherDailyValueProvider.findDailyTemperatureValue(grid, date, isMax);

        // then
        assertThat(result).isEqualTo(expectedMaxTemp);

        // Repository의 특정 메소드가 1번 호출되었는지 검증
        LocalTime expectedTime = LocalTime.of(6, 0);
        verify(weatherRepository, times(1)).findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, date.atTime(expectedTime).atZone(ZoneId.of("Asia/Seoul")).toInstant());
    }

    @Test
    @DisplayName("최저 기온 조회 성공")
    void findDailyTemperatureValue_Min_Success() {
        // given
        boolean isMax = false;
        double expectedMinTemp = 18.0;
        Weather mockWeather = Weather.builder()
                .temperatureMin(expectedMinTemp)
                .build();

        given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(any(Grid.class), any(Instant.class)))
                .willReturn(Optional.of(mockWeather));

        // when
        Double result = weatherDailyValueProvider.findDailyTemperatureValue(grid, date, isMax);

        // then
        assertThat(result).isEqualTo(expectedMinTemp);

        LocalTime expectedTime = LocalTime.of(21, 0);
        verify(weatherRepository, times(1)).findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, date.atTime(expectedTime).atZone(ZoneId.of("Asia/Seoul")).toInstant());
    }

    @Test
    @DisplayName("조회된 날씨 데이터가 없을 경우 null 반환")
    void findDailyTemperatureValue_NotFound_ReturnsNull() {
        // given
        boolean isMax = true;
        // Repository가 비어있는 Optional을 반환하도록 설정
        given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(any(Grid.class), any(Instant.class)))
                .willReturn(Optional.empty());

        // when
        Double result = weatherDailyValueProvider.findDailyTemperatureValue(grid, date, isMax);

        // then
        assertThat(result).isNull();
    }
}