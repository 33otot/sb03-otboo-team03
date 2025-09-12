package com.samsamotot.otboo.location.repository;

import com.samsamotot.otboo.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
}
