package com.samsamotot.otboo.profile.service.impl;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.mapper.ProfileMapper;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final String SERVICE = "[ProfileServiceImpl] ";

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;


    @Override
    @Transactional(readOnly = true)
    public ProfileDto getProfileByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));

        String username = user.getUsername();

        log.info(SERVICE + "사용자 프로필 조회 시도 - 사용자 ID: {}, 사용자 이름: {}",
                userId, username);

        Profile userProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new OtbooException(ErrorCode.PROFILE_NOT_FOUND));

        log.info(SERVICE + "사용자 프로필 조회 성공 - 사용자 ID: {}, 사용자 이름: {}",
                userProfile.getUser().getId(), userProfile.getUser().getUsername());

        return profileMapper.toDto(userProfile);
    }
}
