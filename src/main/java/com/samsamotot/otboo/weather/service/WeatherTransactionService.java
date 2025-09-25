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
        weatherRepository.deleteAllByGrid(grid);
        weatherRepository.flush();
        weatherRepository.saveAll(weatherList);
    }
}