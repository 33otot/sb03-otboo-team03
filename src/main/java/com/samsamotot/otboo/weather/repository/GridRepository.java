package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.weather.entity.Grid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GridRepository extends JpaRepository<Grid, UUID> {

    Optional<Grid> findByXAndY(int x, int y);
}
