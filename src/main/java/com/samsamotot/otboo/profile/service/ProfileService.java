package com.samsamotot.otboo.profile.service;

import com.samsamotot.otboo.profile.dto.ProfileDto;

import java.util.UUID;

public interface ProfileService {

    ProfileDto getProfileByUserId(UUID userId);
}
