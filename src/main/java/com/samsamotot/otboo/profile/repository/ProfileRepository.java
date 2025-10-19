package com.samsamotot.otboo.profile.repository;

import com.samsamotot.otboo.profile.entity.Profile;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(UUID userId);

    List<Profile> findAllByLocationGridId(UUID gridId);

    @Query("select p from Profile p where p.user.id in :userIds")
    List<Profile> findByUserIdIn(@Param("userIds") Collection<UUID> userIds);
}

