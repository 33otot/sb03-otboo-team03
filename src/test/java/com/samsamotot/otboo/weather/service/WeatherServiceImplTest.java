package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.weather.client.KmaClient;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import com.samsamotot.otboo.weather.entity.*;
import com.samsamotot.otboo.weather.mapper.WeatherMapper;
import com.samsamotot.otboo.weather.repository.GridRepository;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import com.samsamotot.otboo.weather.service.impl.WeatherAlterServiceImpl;
import com.samsamotot.otboo.weather.service.impl.WeatherServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherServiceImpl 단위 테스트")
public class WeatherServiceImplTest {

    @InjectMocks
    private WeatherServiceImpl weatherService;

    @Mock
    private KmaClient kmaClient;

    @Mock
    private WeatherTransactionService weatherTransactionService;

    @Mock
    private WeatherAlterServiceImpl weatherAlterService;

    @Mock
    private GridRepository gridRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private WeatherMapper weatherMapper;

    @Mock
    private WeatherRepository weatherRepository;

    @Nested
    @DisplayName("날씨 수집 로직 테스트")
    class fetchServiceTests {

        @Test
        void 날씨_정보_업데이트_성공 () {
            // Given
            Location testLocation = LocationFixture.createValidLocation(); // "서울특별시", "중구", "명동", ""
            Grid grid = testLocation.getGrid();

            // 기상청 API 가짜 응답 데이터
            List<WeatherForecastResponse.Item> fakeItems = List.of(
                    new WeatherForecastResponse.Item("20250823", "0500", "TMP", "20250823", "0800", "25", "60", "127"),
                    new WeatherForecastResponse.Item("20250823", "0500", "REH", "20250823", "0800", "80", "60", "127")
            );

            WeatherForecastResponse fakeFcstResponse = new WeatherForecastResponse(
                    new WeatherForecastResponse.Response(
                            new WeatherForecastResponse.Header("00", "NORMAL_SERVICE"),
                            new WeatherForecastResponse.Body("JSON", new WeatherForecastResponse.Items(fakeItems), 1000, 1, 2)
                    )
            );

            // Mockito를 사용하여 가짜 객체들 행동 정의
            given(gridRepository.findById(grid.getId())).willReturn(Optional.of(grid));
            given(kmaClient.fetchWeather(grid.getX(), grid.getY()))
                    .willReturn(Mono.just(fakeFcstResponse));


            // When
            // @Async로 비동기 처리 join()으로 비동기 작업 끝날 때까지 기다림
            weatherService.updateWeatherForGrid(grid.getId()).join();


            // Then
            // 각 메소드가 정확히 1번 호출 됐나 검증
            verify(weatherTransactionService, times(1)).updateWeather(any(Grid.class), any(List.class));
        }

        @Test
        void 날씨_정보_업데이트_잘못된_요청으로_API_응답_없으면_정상완료 () {
            // Given
            Location testLocation = LocationFixture.createLocationWithCoordinates();
            Grid grid = testLocation.getGrid();

            // 기상청 API의 header는 정상이지만, body의 items가 비어있거나 null인 경우
            WeatherForecastResponse fakeFcstResponse = new WeatherForecastResponse(
                    new WeatherForecastResponse.Response(
                            new WeatherForecastResponse.Header("00", "NORMAL_SERVICE"),
                            new WeatherForecastResponse.Body("JSON", null, 1000, 1, 2) // items가 null
                    )
            );

            // Mockito 설정:
            // kmaClient.fetchWeather가 호출되면, 비어있는 가짜 응답을 반환
            given(gridRepository.findById(grid.getId())).willReturn(Optional.of(grid));
            given(kmaClient.fetchWeather(grid.getX(), grid.getY()))
                    .willReturn(Mono.just(fakeFcstResponse));


            // When
            CompletableFuture<Void> future = weatherService.updateWeatherForGrid(grid.getId());

            // Then
            // WeatherServiceImpl의 exceptionally 블록으로 인해 예외가 발생하지 않고 정상 완료됨
            assertThat(future.isCompletedExceptionally()).isFalse();
            assertThat(future.join()).isNull(); // exceptionally에서 null을 반환

            // 유효하지 않은 데이터이므로 DB 접근 불가
            verify(weatherTransactionService, times(0)).updateWeather(any(Grid.class), any(List.class));

        }

        @Test
        void 데이터_변환_성공 () {
            // Given
            Location testLocation = LocationFixture.createValidLocation();
            Grid grid = testLocation.getGrid();

            // 습도(REH) 예보가 포함되지 않은 데이터
            List<WeatherForecastResponse.Item> fakeItems = List.of(
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "SKY", "20250823", "0800", "1", "60", "127"), // 하늘상태: 맑음
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "POP", "20250823", "0800", "40", "60", "127"), // 강수확률: 40%
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "PTY", "20250823", "0800", "1", "60", "127"), // 강수형태: 비
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "PCP", "20250823", "0800", "10", "60", "127"), // 1시간 강수량: 10mm
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "TMP", "20250823", "0800", "25", "60", "127"), // 1시간 기온: 25도
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "TMN", "20250823", "0800", "15", "60", "127"), // 일 최저기온: 15도
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "TMX", "20250823", "0800", "28", "60", "127"), // 일 최고기온: 28도
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "REH", "20250823", "0800", "40", "60", "127"), // 습도 40%
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "WSD", "20250823", "0800", "3.0", "60", "127") // 풍속: 약한바람 (3m/s)
            );

            WeatherForecastResponse fakeFcstResponse = new WeatherForecastResponse(
                    new WeatherForecastResponse.Response(
                            new WeatherForecastResponse.Header("00", "NORMAL_SERVICE"),
                            new WeatherForecastResponse.Body("JSON", new WeatherForecastResponse.Items(fakeItems), 1000, 1, 2)
                    )
            );

            // Mockito 설정
            given(gridRepository.findById(grid.getId())).willReturn(Optional.of(grid));
            given(kmaClient.fetchWeather(any(Integer.class), any(Integer.class)))
                    .willReturn(Mono.just(fakeFcstResponse));

            // updateWeather 메소드가 호출될 때, 전달되는 List<Weather>를 캡쳐하기 위한 설정
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Weather>> savedWeatherCaptor = ArgumentCaptor.forClass(List.class);


            // When (실행)
            weatherService.updateWeatherForGrid(grid.getId()).join();


            // Then (검증)
            // updateWeatherData가 호출됐는지 확인 후, 전달된 인자 캡쳐
            verify(weatherTransactionService).updateWeather(any(Grid.class), savedWeatherCaptor.capture());

            List<Weather> savedWeathers = savedWeatherCaptor.getValue();

            assertThat(savedWeathers).hasSize(1);
            Weather savedWeather = savedWeathers.get(0);

            assertThat(savedWeather.getGrid()).isEqualTo(grid);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
            ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
            assertThat(savedWeather.getForecastAt()).isEqualTo(LocalDateTime.parse("202508230800", formatter).atZone(seoulZoneId).toInstant());
            assertThat(savedWeather.getForecastedAt()).isEqualTo(LocalDateTime.parse("202508230500", formatter).atZone(seoulZoneId).toInstant());


            assertThat(savedWeather.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);
            assertThat(savedWeather.getPrecipitationProbability()).isEqualTo(40.0);
            assertThat(savedWeather.getPrecipitationType()).isEqualTo(Precipitation.RAIN);
            assertThat(savedWeather.getPrecipitationAmount()).isEqualTo(10.0);
            assertThat(savedWeather.getTemperatureCurrent()).isEqualTo(25.0);
            assertThat(savedWeather.getTemperatureMin()).isEqualTo(15.0);
            assertThat(savedWeather.getTemperatureMax()).isEqualTo(28.0);
            assertThat(savedWeather.getHumidityCurrent()).isEqualTo(40.0);
            assertThat(savedWeather.getWindSpeed()).isEqualTo(3.0);
            assertThat(savedWeather.getWindAsWord()).isEqualTo(WindAsWord.WEAK);

            assertThat(savedWeather.getHumidityComparedToDayBefore()).isNull();
            assertThat(savedWeather.getTemperatureComparedToDayBefore()).isNull();
        }

        @Test
        void 데이터_변환_시_습도_누락된_경우 () {
            // Given
            Location testLocation = LocationFixture.createValidLocation();
            Grid grid = testLocation.getGrid();

            // 습도(REH) 예보가 포함되지 않은 데이터
            List<WeatherForecastResponse.Item> fakeItems = List.of(
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "SKY", "20250823", "0800", "1", "60", "127"), // 하늘상태: 맑음
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "POP", "20250823", "0800", "40", "60", "127"), // 강수확률: 40%
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "PTY", "20250823", "0800", "1", "60", "127"), // 강수형태: 비
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "PCP", "20250823", "0800", "10", "60", "127"), // 1시간 강수량: 10mm
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "TMP", "20250823", "0800", "25", "60", "127"), // 1시간 기온: 25도
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "TMN", "20250823", "0800", "15", "60", "127"), // 일 최저기온: 15도
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "TMX", "20250823", "0800", "28", "60", "127"), // 일 최고기온: 28도
                    new WeatherForecastResponse.Item(
                            "20250823", "0500", "WSD", "20250823", "0800", "3.0", "60", "127") // 풍속: 약한바람 (3m/s)
            );

            WeatherForecastResponse fakeFcstResponse = new WeatherForecastResponse(
                    new WeatherForecastResponse.Response(
                            new WeatherForecastResponse.Header("00", "NORMAL_SERVICE"),
                            new WeatherForecastResponse.Body("JSON", new WeatherForecastResponse.Items(fakeItems), 1000, 1, 2)
                    )
            );

            // Mockito 설정
            given(gridRepository.findById(grid.getId())).willReturn(Optional.of(grid));
            given(kmaClient.fetchWeather(any(Integer.class), any(Integer.class)))
                    .willReturn(Mono.just(fakeFcstResponse));

            // updateWeather 메소드가 호출될 때, 전달되는 List<Weather>를 캡쳐하기 위한 설정
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Weather>> savedWeatherCaptor = ArgumentCaptor.forClass(List.class);


            // When (실행)
            weatherService.updateWeatherForGrid(grid.getId()).join();


            // Then (검증)
            // updateWeatherData가 호출됐는지 확인 후, 전달된 인자 캡쳐
            verify(weatherTransactionService).updateWeather(any(Grid.class), savedWeatherCaptor.capture());

            List<Weather> savedWeathers = savedWeatherCaptor.getValue();

            assertThat(savedWeathers).hasSize(1);
            Weather savedWeather = savedWeathers.get(0);

            assertThat(savedWeather.getGrid()).isEqualTo(grid);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
            ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
            assertThat(savedWeather.getForecastAt()).isEqualTo(LocalDateTime.parse("202508230800", formatter).atZone(seoulZoneId).toInstant());
            assertThat(savedWeather.getForecastedAt()).isEqualTo(LocalDateTime.parse("202508230500", formatter).atZone(seoulZoneId).toInstant());


            assertThat(savedWeather.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);
            assertThat(savedWeather.getPrecipitationProbability()).isEqualTo(40.0);
            assertThat(savedWeather.getPrecipitationType()).isEqualTo(Precipitation.RAIN);
            assertThat(savedWeather.getPrecipitationAmount()).isEqualTo(10.0);
            assertThat(savedWeather.getTemperatureCurrent()).isEqualTo(25.0);
            assertThat(savedWeather.getTemperatureMin()).isEqualTo(15.0);
            assertThat(savedWeather.getTemperatureMax()).isEqualTo(28.0);
            assertThat(savedWeather.getWindSpeed()).isEqualTo(3.0);
            assertThat(savedWeather.getWindAsWord()).isEqualTo(WindAsWord.WEAK);

            assertThat(savedWeather.getHumidityCurrent()).isNull();
            assertThat(savedWeather.getHumidityComparedToDayBefore()).isNull();
            assertThat(savedWeather.getTemperatureComparedToDayBefore()).isNull();
        }

        @Test
        void 데이터_변환_최저_최고_기온이_특정_시간대에만_존재 () {
            // Given
            Location testLocation = LocationFixture.createValidLocation();
            Grid grid = testLocation.getGrid();

            // 05시 예보에는 TMN, TMX가 포함되어 있고, 08시 예보에는 포함되어 있지 않음
            List<WeatherForecastResponse.Item> fakeItems = List.of(
                    // 05시 예보
                    new WeatherForecastResponse.Item("20250924", "0500", "TMN", "20250924", "0500", "15.0", "60", "127"),
                    new WeatherForecastResponse.Item("20250924", "0500", "TMX", "20250924", "0500", "28.0", "60", "127"),
                    new WeatherForecastResponse.Item("20250924", "0500", "TMP", "20250924", "0500", "16.0", "60", "127"),
                    // 08시 예보
                    new WeatherForecastResponse.Item("20250924", "0500", "TMP", "20250924", "0800", "22.0", "60", "127")
            );
            WeatherForecastResponse fakeResponse = new WeatherForecastResponse(
                    new WeatherForecastResponse.Response(
                            new WeatherForecastResponse.Header("00", "NORMAL_SERVICE"),
                            new WeatherForecastResponse.Body("JSON", new WeatherForecastResponse.Items(fakeItems), 1000, 1, 2)
                    )
            );

            given(gridRepository.findById(grid.getId())).willReturn(Optional.of(grid));
            given(kmaClient.fetchWeather(any(Integer.class), any(Integer.class)))
                    .willReturn(Mono.just(fakeResponse));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);

            // When (실행)
            weatherService.updateWeatherForGrid(grid.getId()).join();

            // Then (검증)
            verify(weatherTransactionService).updateWeather(any(Grid.class), captor.capture());
            List<Weather> savedWeathers = captor.getValue();

            // 05시와 08시, 총 2개의 Weather 엔티티가 생성되었는지 확인
            assertThat(savedWeathers).hasSize(2);

            // 05시 예보 데이터 검증
            Weather weatherAt05 = savedWeathers.stream()
                    .filter(w -> {
                        Instant forecastAt = w.getForecastAt();
                        LocalDateTime ldt = forecastAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
                        return ldt.getHour() == 5 && ldt.getMinute() == 0;
                    })
                    .findFirst().orElseThrow();

            assertThat(weatherAt05.getTemperatureMin()).isEqualTo(15.0);
            assertThat(weatherAt05.getTemperatureMax()).isEqualTo(28.0);

            // 08시 예보 데이터 검증
            Weather weatherAt08 = savedWeathers.stream()
                    .filter(w -> {
                        Instant forecastAt = w.getForecastAt();
                        LocalDateTime ldt = forecastAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
                        return ldt.getHour() == 8 && ldt.getMinute() == 0;
                    })
                    .findFirst().orElseThrow();

            // 08시 데이터에는 최저/최고 기온 정보가 없었으므로, 필드가 null인지 확인
            assertThat(weatherAt08.getTemperatureMin()).isNull();
            assertThat(weatherAt08.getTemperatureMax()).isNull();
        }

        @Test
        void 데이터_변환_강수량_값이_강수없음_일_때 () {
            // Given (준비)
            Location testLocation = LocationFixture.createValidLocation();
            Grid grid = testLocation.getGrid();

            // 강수량(PCP) 값이 "강수없음"인 가짜 응답 데이터
            List<WeatherForecastResponse.Item> fakeItems = List.of(
                    new WeatherForecastResponse.Item("20250924", "0500", "PCP", "20250924", "0800", "강수없음", "60", "127")
            );
            WeatherForecastResponse fakeResponse = new WeatherForecastResponse(
                    new WeatherForecastResponse.Response(
                            new WeatherForecastResponse.Header("00", "NORMAL_SERVICE"),
                            new WeatherForecastResponse.Body("JSON", new WeatherForecastResponse.Items(fakeItems), 1000, 1, 1)
                    )
            );

            given(gridRepository.findById(grid.getId())).willReturn(Optional.of(grid));
            given(kmaClient.fetchWeather(any(Integer.class), any(Integer.class)))
                    .willReturn(Mono.just(fakeResponse));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);

            // When (실행)
            weatherService.updateWeatherForGrid(grid.getId()).join();

            // Then (검증)
            verify(weatherTransactionService).updateWeather(any(Grid.class), captor.capture());
            List<Weather> savedWeathers = captor.getValue();

            assertThat(savedWeathers).hasSize(1);
            Weather savedWeather = savedWeathers.get(0);

            // "강수없음" 문자열이 0.0으로 올바르게 변환되었는지 확인
            assertThat(savedWeather.getPrecipitationAmount()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("날씨 목록 조회 테스트")
    class GetWeatherTests {

        @Test
        void DB에_날씨가_있으면_API_호출없이_반환() {
            // Given
            Location location = LocationFixture.createLocation(); // 37.5665, 126.9780
            Grid grid = location.getGrid(); // 60, 127
            WeatherAPILocation locationDto = WeatherAPILocation.builder()
                    .longitude(location.getLongitude())
                    .latitude(location.getLatitude())
                    .x(location.getGrid().getX())
                    .y(location.getGrid().getY())
                    .locationNames(location.getLocationNames())
                    .build();
            Weather weather = WeatherFixture.createWeather(grid);

            Instant now = Instant.now();
            List<Weather> mockWeatherList = List.of(
                    Weather.builder().forecastAt(now.minus(1, ChronoUnit.HOURS)).forecastedAt(now.minus(2, ChronoUnit.HOURS)).grid(grid).temperatureCurrent(20.0).build(),
                    Weather.builder().forecastAt(now.plus(1, ChronoUnit.DAYS)).forecastedAt(now).grid(grid).temperatureCurrent(22.0).build()
                    // ... 더 정교한 테스트를 위해 5일치 이상의 다양한 데이터를 추가할 수 있습니다.
            );
            WeatherDto weatherDto = WeatherFixture.createWeatherDto(weather, locationDto);


            when(locationService.getCurrentLocation(location.getLongitude(), location.getLatitude()))
                    .thenReturn(locationDto);
            when(gridRepository.findByXAndY(grid.getX(), grid.getY()))
                    .thenReturn(Optional.of(grid));
            when(weatherRepository.findAllByGrid(grid))
                    .thenReturn(mockWeatherList);
            when(weatherMapper.toDto(any(Weather.class), eq(locationDto)))
                    .thenReturn(weatherDto);

            // When
            List<WeatherDto> result = weatherService.getWeatherList(location.getLongitude(), location.getLatitude());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.size()).isGreaterThan(0);

            verify(kmaClient, never()).fetchWeather(any(Integer.class), any(Integer.class));
            verify(weatherTransactionService, never()).updateWeather(any(), any());
        }

        @Test
        void DB에_데이터가_없으면_API호출하여_데이터_저장_반환() {
            // Given
            Location location = LocationFixture.createLocation(); // 37.5665, 126.9780
            Grid grid = location.getGrid(); // 60, 127
            WeatherAPILocation locationDto = WeatherAPILocation.builder()
                    .longitude(location.getLongitude())
                    .latitude(location.getLatitude())
                    .x(location.getGrid().getX())
                    .y(location.getGrid().getY())
                    .locationNames(location.getLocationNames())
                    .build();

            // Mock API 응답
            WeatherForecastResponse.Item mockItem = new WeatherForecastResponse.Item(
                    "20250930", "1100", "TMP", "20250930", "1500", "22", "60", "127");
            WeatherForecastResponse.Body mockBody = new WeatherForecastResponse.Body(
                    "JSON", new WeatherForecastResponse.Items(List.of(mockItem)), 1000, 1, 1);
            WeatherForecastResponse.Header mockHeader = new WeatherForecastResponse.Header("00", "NORMAL_SERVICE");
            WeatherForecastResponse mockResponse = new WeatherForecastResponse(new WeatherForecastResponse.Response(mockHeader, mockBody));

            Instant now = Instant.now();
            List<Weather> convertedWeatherList = List.of(
                    Weather.builder().forecastAt(now).forecastedAt(now).grid(grid).temperatureCurrent(22.0).build());

            Weather weather = convertedWeatherList.get(0);

            WeatherDto weatherDto = WeatherFixture.createWeatherDto(weather, locationDto);
            when(locationService.getCurrentLocation(location.getLongitude(), location.getLatitude()))
                    .thenReturn(locationDto);
            when(gridRepository.findByXAndY(locationDto.x(), locationDto.y()))
                    .thenReturn(Optional.of(grid));
            // 처음엔 빈 리스트, 그 다음엔 저장된 데이터 리스트 반환
            when(weatherRepository.findAllByGrid(grid))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(convertedWeatherList);

            when(kmaClient.fetchWeather(grid.getX(), grid.getY()))
                    .thenReturn(Mono.just(mockResponse));
            doNothing().when(weatherTransactionService).updateWeather(any(Grid.class), anyList());
            when(weatherMapper.toDto(any(Weather.class), eq(locationDto)))
                    .thenReturn(weatherDto);

            // When
            List<WeatherDto> result = weatherService.getWeatherList(location.getLongitude(), location.getLatitude());

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            verify(kmaClient, times(1)).fetchWeather(grid.getX(), grid.getY());
            verify(weatherTransactionService, times(1)).updateWeather(eq(grid), anyList());
            verify(weatherRepository, times(2)).findAllByGrid(eq(grid));
        }

        @Test
        void 유효하지_않은_좌표로_Grid를_찾지_못하면_OtbooException_GRID_NOT_FOUND() {
            // Given
            Location invalidGridLocation = LocationFixture.createLocationWithZeroCoordinates();
            double longitude = invalidGridLocation.getLongitude();
            double latitude = invalidGridLocation.getLatitude();
            Grid grid = invalidGridLocation.getGrid();
            WeatherAPILocation invalidLocationDto = new WeatherAPILocation(
                    longitude, latitude,
                    grid.getX(), grid.getY(),
                    invalidGridLocation.getLocationNames());

            when(locationService.getCurrentLocation(longitude, latitude))
                    .thenReturn(invalidLocationDto);
            when(gridRepository.findByXAndY(grid.getX(), grid.getY()))
                    .thenReturn(Optional.empty());

            // When
            // Then
            assertThatThrownBy(() -> weatherService.getWeatherList(longitude, latitude))
                    .isInstanceOf(OtbooException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_GRID);
        }

        @Test
        void 성공적으로_비동기_날씨_업데이트_수행() throws ExecutionException, InterruptedException {
            // Given
            Grid grid = GridFixture.createGrid();
            WeatherForecastResponse mockResponse = new WeatherForecastResponse(
                    new WeatherForecastResponse.Response(
                            new WeatherForecastResponse.Header("00", "NORMAL_SERVICE"),
                            new WeatherForecastResponse.Body("JSON", new WeatherForecastResponse.Items(
                                    List.of(new WeatherForecastResponse.Item("20250930", "0600", "TMP", "20250930", "0700", "22", "60", "127"))
                                    ),
                                    1, 1, 10))
                            );

            when(gridRepository.findById(grid.getId()))
                    .thenReturn(Optional.of(grid));
            when(kmaClient.fetchWeather(grid.getX(), grid.getY()))
                    .thenReturn(Mono.just(mockResponse));
            doNothing().when(weatherTransactionService).updateWeather(any(Grid.class), anyList());

            // When
            CompletableFuture<Void> future = weatherService.updateWeatherForGrid(grid.getId());

            // Then
            future.get();

            verify(weatherTransactionService, times(1)).updateWeather(eq(grid), anyList());
        }
    }
}
