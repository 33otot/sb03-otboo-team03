package com.samsamotot.otboo.weather.client;

import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import reactor.core.publisher.Mono;

public interface WeatherClient {

    Mono<WeatherForecastResponse> fetchWeather(int nx, int ny);

}
