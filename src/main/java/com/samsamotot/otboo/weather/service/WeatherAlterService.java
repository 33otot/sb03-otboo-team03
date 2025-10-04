package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.weather.entity.Weather;

public interface WeatherAlterService {

    void checkAndSendAlerts(Weather newWeather);
}
