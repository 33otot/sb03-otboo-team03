package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.client.KmaClient;
import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import com.samsamotot.otboo.weather.entity.*;
import com.samsamotot.otboo.weather.service.impl.WeatherServiceImpl;
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherServiceImpl 단위 테스트")
public class WeatherServiceImplTest {

    @InjectMocks
    private WeatherServiceImpl weatherService;

    @Mock
    private KmaClient weatherKmaClient;


    @Mock
    private WeatherTransactionService weatherTransactionService;

    @Test
    void 날씨_정보_업데이트_성공() {
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
        given(weatherKmaClient.fetchWeather(grid.getX(), grid.getY()))
                .willReturn(Mono.just(fakeFcstResponse));


        // When
        // @Async로 비동기 처리 join()으로 비동기 작업 끝날 때까지 기다림
        weatherService.updateWeatherDataForGrid(grid).join();


        // Then
        // 각 메소드가 정확히 1번 호출 됐나 검증
        verify(weatherTransactionService, times(1)).updateWeatherData(any(Grid.class), any(List.class));
    }

    @Test
    void 날씨_정보_업데이트_잘못된_요청으로_API_응답_없으면_정상완료() {
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
        // weatherKmaClient.fetchWeather가 호출되면, 비어있는 가짜 응답을 반환
        given(weatherKmaClient.fetchWeather(grid.getX(), grid.getY()))
                .willReturn(Mono.just(fakeFcstResponse));


        // When
        CompletableFuture<Void> future = weatherService.updateWeatherDataForGrid(grid);
        
        // Then
        // WeatherServiceImpl의 exceptionally 블록으로 인해 예외가 발생하지 않고 정상 완료됨
        assertThat(future.isCompletedExceptionally()).isFalse();
        assertThat(future.join()).isNull(); // exceptionally에서 null을 반환

        // 유효하지 않은 데이터이므로 DB 접근 불가
        verify(weatherTransactionService, times(0)).updateWeatherData(any(Grid.class), any(List.class));

    }

    @Test
    void 날씨_정보_업데이트_API_호출_실패하면_정상완료() {
        // Given
        Location testLocation = LocationFixture.createValidLocation();
        Grid grid = testLocation.getGrid();

        // API 호출 실패 시 발생할 가짜 예외 객체
        RuntimeException apiException = new RuntimeException("API 서버에 연결할 수 없습니다.");

        // weatherKmaClient.fetchWeather가 호출되면, Mono.error()을 통해 예외 발생
        given(weatherKmaClient.fetchWeather(grid.getX(), grid.getY()))
                .willReturn(Mono.error(apiException));


        // When
        CompletableFuture<Void> future = weatherService.updateWeatherDataForGrid(grid);

        // Then
        // WeatherServiceImpl의 exceptionally 블록으로 인해 예외가 발생하지 않고 정상 완료됨
        assertThat(future.isCompletedExceptionally()).isFalse();
        assertThat(future.join()).isNull(); // exceptionally에서 null을 반환

        // API 호출 자체가 실패했으므로 DB 접근 불가
        verify(weatherTransactionService, times(0)).updateWeatherData(any(Grid.class), any(List.class));
    }

    @Test
    void 데이터_변환_성공() {
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
        given(weatherKmaClient.fetchWeather(any(Integer.class), any(Integer.class)))
                .willReturn(Mono.just(fakeFcstResponse));

        // updateWeatherData 메소드가 호출될 때, 전달되는 List<Weather>를 캡쳐하기 위한 설정
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Weather>> savedWeatherCaptor = ArgumentCaptor.forClass(List.class);


        // When (실행)
        weatherService.updateWeatherDataForGrid(grid).join();


        // Then (검증)
        // updateWeatherData가 호출됐는지 확인 후, 전달된 인자 캡쳐
        verify(weatherTransactionService).updateWeatherData(any(Grid.class), savedWeatherCaptor.capture());

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
    void 데이터_변환_시_습도_누락된_경우() {
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
        given(weatherKmaClient.fetchWeather(any(Integer.class), any(Integer.class)))
                .willReturn(Mono.just(fakeFcstResponse));

        // updateWeatherData 메소드가 호출될 때, 전달되는 List<Weather>를 캡쳐하기 위한 설정
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Weather>> savedWeatherCaptor = ArgumentCaptor.forClass(List.class);


        // When (실행)
        weatherService.updateWeatherDataForGrid(grid).join();


        // Then (검증)
        // updateWeatherData가 호출됐는지 확인 후, 전달된 인자 캡쳐
        verify(weatherTransactionService).updateWeatherData(any(Grid.class), savedWeatherCaptor.capture());

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
    void 데이터_변환_최저_최고_기온이_특정_시간대에만_존재() {
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

        given(weatherKmaClient.fetchWeather(any(Integer.class), any(Integer.class)))
                .willReturn(Mono.just(fakeResponse));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);

        // When (실행)
        weatherService.updateWeatherDataForGrid(grid).join();

        // Then (검증)
        verify(weatherTransactionService).updateWeatherData(any(Grid.class), captor.capture());
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
    void 데이터_변환_강수량_값이_강수없음_일_때() {
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

        given(weatherKmaClient.fetchWeather(any(Integer.class), any(Integer.class)))
                .willReturn(Mono.just(fakeResponse));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);

        // When (실행)
        weatherService.updateWeatherDataForGrid(grid).join();

        // Then (검증)
        verify(weatherTransactionService).updateWeatherData(any(Grid.class), captor.capture());
        List<Weather> savedWeathers = captor.getValue();

        assertThat(savedWeathers).hasSize(1);
        Weather savedWeather = savedWeathers.get(0);

        // "강수없음" 문자열이 0.0으로 올바르게 변환되었는지 확인
        assertThat(savedWeather.getPrecipitationAmount()).isEqualTo(0.0);
    }
}
