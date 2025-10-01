package com.samsamotot.otboo.location.repository;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.entity.Grid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    @Query("SELECT l FROM Location l " +
            "WHERE l.longitude = :longitude " +
            "AND l.latitude = :latitude")
    Optional<Location> findByLongitudeAndLatitude(double longitude, double latitude);

    @Query("SELECT l.grid FROM Location l " +
            "WHERE l.longitude = :longitude " +
            "AND l.latitude = :latitude")
    Optional<Grid> findGridByLongitudeAndLatitude(double longitude, double latitude);

    @Query("SELECT l FROM Location l " +
            "JOIN FETCH l.grid")
    List<Location> findAllWithGrid();
}
