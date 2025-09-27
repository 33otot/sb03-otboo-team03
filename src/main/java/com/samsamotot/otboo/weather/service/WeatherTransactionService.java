package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WeatherTransactionService {
    
    private final WeatherRepository weatherRepository;
    private final FeedRepository feedRepository;
    
    public void updateWeatherData(Grid grid, List<Weather> newWeatherList) {
        // 새 데이터가 비어있으면 기존 데이터를 보존합니다.
        if (newWeatherList == null || newWeatherList.isEmpty()) {
            return;
        }

        // --- 1. '오래된 데이터'만 삭제 ---
        // '어제' 데이터와 비교를 위해, 최소 2일치 데이터 유지
        Instant deleteThreshold = Instant.now().minus(3, ChronoUnit.DAYS);
        weatherRepository.deleteByGridAndForecastAtBefore(grid, deleteThreshold);

        // --- 2. '전날' 데이터 조회 준비 ---
        LocalDate minDate = newWeatherList.stream()
                .map(w -> LocalDate.ofInstant(w.getForecastAt(), ZoneId.of("Asia/Seoul")))
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now(ZoneId.of("Asia/Seoul")));

        // '어제' 날씨 데이터를 미리 DB에서 조회하여 Map으로 만듦
        // (minDate - 1일) 00시부터 (minDate) 00시 전까지의 데이터 조회
        Instant yesterdayStart = minDate.minusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        Instant yesterdayEnd = minDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();

        Map<LocalTime, Weather> yesterdayWeatherMap = weatherRepository
                .findByGridAndForecastAtBetween(grid, yesterdayStart, yesterdayEnd)
                .stream()
                .collect(Collectors.toMap(
                        w -> LocalTime.ofInstant(w.getForecastAt(), ZoneId.of("Asia/Seoul")),
                        w -> w
                ));

        // --- 3. 새로 수집한 데이터에 '전날 대비 값' 계산 및 설정 ---
        for (Weather todayWeather : newWeatherList) {
            LocalTime todayTime = LocalTime.ofInstant(todayWeather.getForecastAt(), ZoneId.of("Asia/Seoul"));

            // yesterdayWeatherMap에서 '같은 시간' 어제 데이터 가져옴
            Weather yesterdayWeather = yesterdayWeatherMap.get(todayTime);

            if (yesterdayWeather != null) {
                // '전날 대비 값' 계산하여 엔티티에 설정 (1단계에서 추가한 Setter 사용)
                Double tempCompared = todayWeather.getTemperatureCurrent() - yesterdayWeather.getTemperatureCurrent();
                todayWeather.setTemperatureComparedToDayBefore(tempCompared);

                Double humidCompared = todayWeather.getHumidityCurrent() - yesterdayWeather.getHumidityCurrent();
                todayWeather.setHumidityComparedToDayBefore(humidCompared);
            }
        }

        // --- 4. 최종 데이터 DB에 저장(UPSERT) ---
        weatherRepository.saveAll(newWeatherList);
    }
}