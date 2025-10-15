package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeatherTransactionServiceTest {

    @InjectMocks
    private WeatherTransactionService weatherTransactionService;

    @Mock
    private WeatherRepository weatherRepository;

    private Grid grid;
    private Instant now;

    @BeforeEach
    void setUp() {
        grid = GridFixture.createGrid();
        now = Instant.now();
    }

    @Nested
    @DisplayName("WeatherTransactionService updateWeather 메소드 테스트")
    class UpdateWeathers {

        @Test
        void 새로운_날씨_데이터가_null_이거나_비어있으면_작업_수행하지_않음() {
            // When
            weatherTransactionService.updateWeather(grid, null);
            weatherTransactionService.updateWeather(grid, new ArrayList<>());

            // Then
            verify(weatherRepository, never()).saveAll(any());
            verify(weatherRepository, never()).deleteByGridAndForecastedAt(any(), any());
            verify(weatherRepository, never()).deleteByGridAndForecastAtBefore(any(), any());
        }

        @Test
        void 전날_데이터가_있을_경우_전날_대비_기온_습도_계산하여_저장() {

            // Given
            Weather todayWeather = WeatherFixture.createWeather(now.plus(1, ChronoUnit.HOURS), now, 25.0, 60.0);
            Weather tomorrowWeather = WeatherFixture.createWeather(now.plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS), now, 27.0, 65.0);
            List<Weather> newWeatherList = new ArrayList<>(List.of(todayWeather, tomorrowWeather));

            Instant yesterdayForecastAt = todayWeather.getForecastAt().minus(1, ChronoUnit.DAYS);
            Weather yesterdayWeather = WeatherFixture.createWeather(yesterdayForecastAt, now.minus(1, ChronoUnit.DAYS), 22.0, 55.0);

            given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, yesterdayForecastAt))
                    .willReturn(Optional.of(yesterdayWeather));

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            assertThat(todayWeather.getTemperatureComparedToDayBefore()).isEqualTo(3.0);
            assertThat(todayWeather.getHumidityComparedToDayBefore()).isEqualTo(5.0);
            assertThat(tomorrowWeather.getTemperatureComparedToDayBefore()).isEqualTo(2.0);
            assertThat(tomorrowWeather.getHumidityComparedToDayBefore()).isEqualTo(5.0);

            verify(weatherRepository).deleteByGridAndForecastedAt(eq(grid), eq(now));
            verify(weatherRepository).deleteByGridAndForecastAtBefore(eq(grid), any(Instant.class));
            verify(weatherRepository).saveAll(eq(newWeatherList));
        }

        @Test
        void 최고_최저_기온이_null이면_DB에서_조회하여_채워넣는다() {

            // Given
            List<Weather> newWeatherList = new ArrayList<>();
            Weather weatherWithNullTemps = Weather.builder()
                    .grid(grid)
                    .forecastAt(now)
                    .forecastedAt(now)
                    .temperatureCurrent(25.0)
                    .temperatureMax(null)
                    .temperatureMin(null)
                    .build();
            newWeatherList.add(weatherWithNullTemps);

            given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(eq(grid), any(Instant.class)))
                    .willReturn(Optional.empty());

            LocalDate date = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
            Instant maxTempTime = date.atTime(LocalTime.of(6, 0)).atZone(ZoneId.of("Asia/Seoul")).toInstant();
            Instant minTempTime = date.atTime(LocalTime.of(21, 0)).atZone(ZoneId.of("Asia/Seoul")).toInstant();

            Weather maxTempWeather = WeatherFixture.createWeatherWithMaxMinTemp(
                    maxTempTime, now.minus(1, ChronoUnit.HOURS), 30.0, null);
            Weather minTempWeather = WeatherFixture.createWeatherWithMaxMinTemp(
                    minTempTime, now.minus(1, ChronoUnit.HOURS), null, 15.0);

            given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, maxTempTime))
                    .willReturn(Optional.of(maxTempWeather));
            given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, minTempTime))
                    .willReturn(Optional.of(minTempWeather));

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);
            verify(weatherRepository).saveAll(captor.capture());

            Weather savedWeather = captor.getValue().get(0);
            assertThat(savedWeather.getTemperatureMax()).isEqualTo(30.0);
            assertThat(savedWeather.getTemperatureMin()).isEqualTo(15.0);
        }

        @Test
        void DB_정리_로직이_정확한_인자와_함께_호출() {

            // Given
            List<Weather> newWeatherList = new ArrayList<>(List.of(
                    WeatherFixture.createWeather(now.plus(1, ChronoUnit.HOURS), now, 25.0, 60.0)
            ));

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            verify(weatherRepository).deleteByGridAndForecastedAt(grid, now);

            ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
            verify(weatherRepository).deleteByGridAndForecastAtBefore(eq(grid), captor.capture());

            Instant captureThreshold = captor.getValue();
            long daysDifference = ChronoUnit.DAYS.between(captureThreshold, Instant.now());
            assertThat(daysDifference).isEqualTo(3);
        }
    }
}