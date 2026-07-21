package com.samsamotot.otboo.profile.repository;

import com.samsamotot.otboo.profile.entity.Profile;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID>, ProfileRepositoryCustom {
    Optional<Profile> findByUserId(UUID userId);
}

