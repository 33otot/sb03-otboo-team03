package com.samsamotot.otboo.location.repository;

import com.samsamotot.otboo.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    @Query("SELECT l FROM Location l " +
            "WHERE l.longitude = :longitude " +
            "AND l.latitude = :latitude")
    Optional<Location> findByLongitudeAndLatitude(double longitude, double latitude);
}
