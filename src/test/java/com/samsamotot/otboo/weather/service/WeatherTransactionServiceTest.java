package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherTransactionServiceTest {

    @InjectMocks
    private WeatherTransactionService weatherTransactionService;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private WeatherDailyValueProvider weatherDailyValueProvider;

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
            verify(weatherRepository, never()).deleteOldAndUnreferencedWeather(any(), any());
            verify(weatherRepository, never()).deleteOutdatedAndUnreferencedWeather(any());
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

            verify(weatherRepository).deleteOldAndUnreferencedWeather(eq(grid), any());
            verify(weatherRepository).deleteOutdatedAndUnreferencedWeather(eq(grid));
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

            LocalDate date = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();

            given(weatherDailyValueProvider.findDailyTemperatureValue(grid, date, true)).willReturn(30.0);
            given(weatherDailyValueProvider.findDailyTemperatureValue(grid, date, false)).willReturn(15.0);

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            ArgumentCaptor<Weather> captor = ArgumentCaptor.forClass(Weather.class);
            verify(weatherRepository).save(captor.capture()); // 인자 값 캡쳐

            Weather savedWeather = captor.getValue();
            assertThat(savedWeather.getTemperatureMax()).isEqualTo(30.0);
            assertThat(savedWeather.getTemperatureMin()).isEqualTo(15.0);
        }

        @Test
        void 기존_날씨_데이터가_있으면_업데이트() {
            // Given
            Weather newWeather = WeatherFixture.createWeather(now.plus(1, ChronoUnit.HOURS), now, 25.0, 60.0);
            Weather existingWeather = WeatherFixture.createWeather(now.plus(1, ChronoUnit.DAYS), now, 27.0, 65.0);
            List<Weather> newWeatherList = new ArrayList<>(List.of(newWeather));

            given(weatherRepository.findByGridAndForecastedAtAndForecastAt(
                    newWeather.getGrid(), newWeather.getForecastedAt(), newWeather.getForecastAt()))
                    .willReturn(Optional.of(existingWeather));

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            verify(weatherRepository, times(0)).save(any(Weather.class));
            assertThat(existingWeather.getTemperatureCurrent()).isEqualTo(newWeather.getTemperatureCurrent());
            assertThat(existingWeather.getHumidityCurrent()).isEqualTo(newWeather.getHumidityCurrent());
        }

        @Test
        void 최고_최저_기온이_이미_있으면_Provider_호출_안함() {
            // Given
            // 최고/최저 기온이 이미 채워진 날씨 데이터
            Weather weatherWithTemps = Weather.builder()
                    .grid(grid)
                    .forecastAt(now)
                    .forecastedAt(now)
                    .temperatureMax(30.0)
                    .temperatureMin(15.0)
                    .build();
            List<Weather> newWeatherList = new ArrayList<>(List.of(weatherWithTemps));

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            // weatherDailyValueProvider가 한 번도 호출되지 않았는지 검증
            verify(weatherDailyValueProvider, never()).findDailyTemperatureValue(any(Grid.class), any(LocalDate.class), anyBoolean());
        }

        @Test
        void 데이터_삽입_경합_발생_시_업데이트로_전환() {
            // Given
            Weather newWeather = WeatherFixture.createWeather(now.plus(1, ChronoUnit.HOURS), now, 25.0, 60.0);
            Weather raceConditionWinner = WeatherFixture.createWeather(now.plus(1, ChronoUnit.HOURS), now, 24.0, 59.0);
            List<Weather> newWeatherList = new ArrayList<>(List.of(newWeather));

            // findBy...가 처음 호출되면 Optional.empty(), 두 번째 호출되면 raceConditionWinner를 반환하도록 설정
            given(weatherRepository.findByGridAndForecastedAtAndForecastAt(
                    newWeather.getGrid(), newWeather.getForecastedAt(), newWeather.getForecastAt()))
                    .willReturn(Optional.empty(), Optional.of(raceConditionWinner));

            // 2. save() 시도 시, 다른 스레드가 먼저 삽입하여 UNIQUE 제약조건 위반 예외가 발생했다고 가정
            doThrow(DataIntegrityViolationException.class).when(weatherRepository).save(newWeather);

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            // save는 실패했지만, 최종적으로 업데이트 로직이 수행되었는지 확인
            assertThat(raceConditionWinner.getTemperatureCurrent()).isEqualTo(newWeather.getTemperatureCurrent());
            assertThat(raceConditionWinner.getHumidityCurrent()).isEqualTo(newWeather.getHumidityCurrent());
            verify(weatherRepository, times(1)).save(any(Weather.class)); // save는 1번 시도됨
        }

        @Test
        void 전날_데이터의_필드가_null이면_비교값도_null() {
            // Given
            Weather todayWeather = WeatherFixture.createWeather(now.plus(1, ChronoUnit.HOURS), now, 25.0, 60.0);
            Instant yesterdayForecastAt = todayWeather.getForecastAt().minus(1, ChronoUnit.DAYS);
            // 기온과 습도가 null인 어제 날씨 데이터
            Weather yesterdayWeather = Weather.builder().forecastAt(yesterdayForecastAt).temperatureCurrent(null).humidityCurrent(null).build();
            List<Weather> newWeatherList = new ArrayList<>(List.of(todayWeather));

            given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, yesterdayForecastAt))
                    .willReturn(Optional.of(yesterdayWeather));

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            assertThat(todayWeather.getTemperatureComparedToDayBefore()).isNull();
            assertThat(todayWeather.getHumidityComparedToDayBefore()).isNull();
        }

        @Test
        void DB_정리_로직이_정확한_인자와_함께_호출() {

            // Given
            List<Weather> newWeatherList = new ArrayList<>(List.of(
                    WeatherFixture.createWeather(now.plus(1, ChronoUnit.HOURS), now, 25.0, 60.0)
            ));

            given(weatherRepository.deleteOldAndUnreferencedWeather(any(Grid.class), any(Instant.class))).willReturn(1);
            given(weatherRepository.deleteOutdatedAndUnreferencedWeather(any(Grid.class))).willReturn(2);

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            // 1. 3일 이상 지난 데이터 삭제 메서드 호출 검증
            ArgumentCaptor<Instant> oldThresholdCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(weatherRepository).deleteOldAndUnreferencedWeather(eq(grid), oldThresholdCaptor.capture());

            // 임계값이 대략 3일 전인지 확인 (테스트 실행 시간에 따른 오차 감안)
            long daysDifference = ChronoUnit.DAYS.between(oldThresholdCaptor.getValue(), Instant.now());
            assertThat(daysDifference).isLessThanOrEqualTo(3);

            // 2. 오래된 발표 시각 데이터 삭제 메서드 호출 검증
            verify(weatherRepository).deleteOutdatedAndUnreferencedWeather(eq(grid));
        }

        @Test
        @DisplayName("전날 데이터가 없을 경우 비교 값은 null로 유지")
        void 전날_데이터가_없으면_비교값은_null_유지() {
            // Given
            Weather todayWeather = WeatherFixture.createWeather(now.plus(1, ChronoUnit.HOURS), now, 25.0, 60.0);
            List<Weather> newWeatherList = new ArrayList<>(List.of(todayWeather));

            // 어제 날씨 데이터가 없도록 설정 (Optional.empty() 반환)
            given(weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(any(Grid.class), any(Instant.class)))
                    .willReturn(Optional.empty());

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            // 비교 값이 null로 유지되었는지 확인
            assertThat(todayWeather.getTemperatureComparedToDayBefore()).isNull();
            assertThat(todayWeather.getHumidityComparedToDayBefore()).isNull();
        }

        @Test
        @DisplayName("최고/최저 기온 조회 결과가 null일 경우 기존 null 값 유지")
        void 최고_최저_기온_조회값이_null이면_null_유지() {
            // Given
            List<Weather> newWeatherList = new ArrayList<>();
            Weather weatherWithNullTemps = Weather.builder()
                    .grid(grid)
                    .forecastAt(now)
                    .forecastedAt(now)
                    .temperatureMax(null)
                    .temperatureMin(null)
                    .build();
            newWeatherList.add(weatherWithNullTemps);

            // DailyValueProvider가 null을 반환하도록 설정
            given(weatherDailyValueProvider.findDailyTemperatureValue(any(Grid.class), any(LocalDate.class), any(boolean.class))).willReturn(null);

            // When
            weatherTransactionService.updateWeather(grid, newWeatherList);

            // Then
            assertThat(weatherWithNullTemps.getTemperatureMax()).isNull();
            assertThat(weatherWithNullTemps.getTemperatureMin()).isNull();
        }
    }
}