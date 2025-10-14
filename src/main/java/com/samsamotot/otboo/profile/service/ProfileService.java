package com.samsamotot.otboo.profile.service;

import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.NotificationSettingUpdateRequest;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProfileService {

    ProfileDto getProfileByUserId(UUID userId);

    ProfileDto updateProfile(UUID userId, @Valid ProfileUpdateRequest request, MultipartFile profileImage);

    void updateNotificationEnabled(UUID userId, NotificationSettingUpdateRequest request);
}
