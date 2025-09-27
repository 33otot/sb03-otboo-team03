package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WeatherTransactionService {
    
    private final WeatherRepository weatherRepository;
    private final FeedRepository feedRepository;
    
    public void updateWeatherData(Grid grid, List<Weather> weatherList) {
        // 새 데이터가 비어있으면 기존 데이터를 보존합니다.
        if (weatherList == null || weatherList.isEmpty()) {
            return;
        }
        
        // 피드에서 참조하는 날씨 데이터 ID들을 조회
        Set<UUID> referencedWeatherIds = feedRepository.findWeatherIdsByGrid(grid);
        
        // 피드에서 참조하지 않는 날씨 데이터만 삭제
        List<Weather> existingWeathers = weatherRepository.findByGrid(grid);
        List<Weather> weathersToDelete = existingWeathers.stream()
                .filter(weather -> !referencedWeatherIds.contains(weather.getId()))
                .collect(Collectors.toList());
        
        if (!weathersToDelete.isEmpty()) {
            weatherRepository.deleteAll(weathersToDelete);
            weatherRepository.flush();
        }
        
        // 새 데이터 저장
        weatherRepository.saveAll(weatherList);
    }
}