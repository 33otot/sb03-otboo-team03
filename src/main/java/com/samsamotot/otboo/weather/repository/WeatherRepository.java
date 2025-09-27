package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    void deleteAllByGrid(Grid grid);

    List<Weather> findAllByGrid(Grid grid);
}
