package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WeatherTransactionService {
    
    private final WeatherRepository weatherRepository;
    
    public void updateWeatherData(Grid grid, List<Weather> weatherList) {
        // 새 데이터가 비어있으면 기존 데이터를 보존합니다.
        if (weatherList == null || weatherList.isEmpty()) {
            return;
        }
        weatherRepository.deleteAllByGrid(grid);
        weatherRepository.flush();
        weatherRepository.saveAll(weatherList);
    }
}