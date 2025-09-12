package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {
}
