package com.samsamotot.otboo.location.repository;

import com.samsamotot.otboo.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<UUID, Location> {
}
