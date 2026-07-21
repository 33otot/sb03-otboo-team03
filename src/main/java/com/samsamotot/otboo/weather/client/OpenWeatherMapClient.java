package com.samsamotot.otboo.weather.client;

import com.samsamotot.otboo.weather.dto.OpenWeatherMapResponse;
import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import com.samsamotot.otboo.weather.util.GridConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("openWeatherMapClient")
public class OpenWeatherMapClient implements WeatherClient {

    private final WebClient webClient;
    private final GridConverter gridConverter;
    private final String apiKey;
    private final String baseUrl;

    public OpenWeatherMapClient(@Qualifier("owmWebClient") WebClient webClient,
                                GridConverter gridConverter,
                                @Value("${weather.open.service-key}") String apiKey,
                                @Value("${weather.open.base-url}") String baseUrl) {
        this.webClient = webClient;
        this.gridConverter = gridConverter;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public Mono<WeatherForecastResponse> fetchWeather(int nx, int ny) {
        // 1. Grid(nx, ny) -> Lat/Lon 변환 (Location 데이터 기반 호출 효과)
        GridConverter.LatLon latLon = gridConverter.toLatLon(nx, ny);

        // 2. OpenWeatherMap API 호출
        return webClient.get()
                .uri(uriBuilder -> UriComponentsBuilder.fromUriString(baseUrl)
                        .path("/weather") 
                        .queryParam("lat", latLon.latitude())
                        .queryParam("lon", latLon.longitude())
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric") // 섭씨 온도
                        .build()
                        .toUri())
                .retrieve()
                .bodyToMono(OpenWeatherMapResponse.class)
                .map(response -> mapToWeatherForecastResponse(response, nx, ny))
                .doOnError(e -> log.error("OpenWeatherMap API Error: {}", e.getMessage()));
    }

    // OpenWeatherMap 응답을 기상청 포맷(WeatherForecastResponse)으로 변환
    private WeatherForecastResponse mapToWeatherForecastResponse(OpenWeatherMapResponse owm, int nx, int ny) {
        // 핵심 데이터(Main)가 없으면 매핑 불가 -> 에러 처리
        if (owm.main() == null) {
            throw new RuntimeException("OpenWeatherMap API Response 'main' is null");
        }

        List<WeatherForecastResponse.Item> items = new ArrayList<>();
        
        // 3시간 간격으로 시간 정규화 (KMA 기준에 맞춤)
        BaseDateTime baseDateTime = calculateBaseDateTime();
        String date = baseDateTime.baseDate;
        String time = baseDateTime.baseTime;

        // 1. TMP (기온)
        items.add(createItem("TMP", String.valueOf(owm.main().temp()), nx, ny, date, time));

        // 2. REH (습도)
        items.add(createItem("REH", String.valueOf(owm.main().humidity()), nx, ny, date, time));

        // 3. WSD (풍속)
        items.add(createItem("WSD", String.valueOf(owm.wind().speed()), nx, ny, date, time));

        // 4. SKY (하늘상태) 매핑
        // OWM: 800(Clear), 80x(Clouds) -> KMA: 1(맑음), 3(구름많음), 4(흐림)
        String sky = "4"; // 기본 흐림
        if (owm.weather() != null && !owm.weather().isEmpty()) {
            int id = owm.weather().get(0).id();
            if (id == 800) sky = "1";
            else if (id > 800 && id <= 802) sky = "3";
        }
        items.add(createItem("SKY", sky, nx, ny, date, time));

        // 5. PTY (강수형태) 매핑
        // OWM Main: Rain, Snow, Drizzle, Thunderstorm -> KMA: 0(없음), 1(비), 2(비/눈), 3(눈), 4(소나기)
        String pty = "0";
        if (owm.weather() != null && !owm.weather().isEmpty()) {
            String main = owm.weather().get(0).main();
            if ("Rain".equalsIgnoreCase(main) || "Drizzle".equalsIgnoreCase(main)) pty = "1";
            else if ("Snow".equalsIgnoreCase(main)) pty = "3";
            else if ("Thunderstorm".equalsIgnoreCase(main)) pty = "4";
        }
        items.add(createItem("PTY", pty, nx, ny, date, time));

        // Response 객체 조립
        WeatherForecastResponse.Items itemsObj = new WeatherForecastResponse.Items(items);
        WeatherForecastResponse.Body body = new WeatherForecastResponse.Body("JSON", itemsObj, items.size(), 1, items.size());
        WeatherForecastResponse.Header header = new WeatherForecastResponse.Header("00", "NORMAL_SERVICE");
        return new WeatherForecastResponse(new WeatherForecastResponse.Response(header, body));
    }

    private WeatherForecastResponse.Item createItem(String category, String value, int nx, int ny, String date, String time) {
        return new WeatherForecastResponse.Item(
                date, time, category, date, time, value, String.valueOf(nx), String.valueOf(ny)
        );
    }

    /**
     * 현재 시간을 기준으로 가장 가까운 과거의 3시간 간격(02, 05, ... 23) 기준 시간을 계산합니다.
     * 예: 14:20 -> 14:00, 13:50 -> 11:00
     */
    private BaseDateTime calculateBaseDateTime() {
        // 서버 시간대와 관계없이 무조건 '서울 시간' 기준
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        int hour = now.getHour();

        // 3시간 간격으로 내림 처리 (Base: 2시)
        // (hour - 2) / 3 * 3 + 2 공식을 사용하면 2, 5, 8... 로 매핑됨
        // 단, 0시, 1시의 경우 전날 23시로 처리해야 함
        
        if (hour < 2) {
            now = now.minusDays(1);
            hour = 23;
        } else {
            hour = ((hour - 2) / 3) * 3 + 2;
        }

        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = String.format("%02d00", hour);

        return new BaseDateTime(baseDate, baseTime);
    }

    private record BaseDateTime(String baseDate, String baseTime) { }
}
