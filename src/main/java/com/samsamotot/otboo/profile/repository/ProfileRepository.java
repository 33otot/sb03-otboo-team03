package com.samsamotot.otboo.profile.repository;

import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(UUID userId);
}
