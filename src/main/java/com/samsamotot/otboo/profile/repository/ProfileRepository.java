package com.samsamotot.otboo.profile.repository;

import com.samsamotot.otboo.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<UUID, Profile> {
}
