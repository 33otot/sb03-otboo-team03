package com.samsamotot.otboo.profile.service.impl;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.storage.S3ImageStorage;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
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
import org.springframework.web.multipart.MultipartFile;

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
    private final S3ImageStorage s3ImageStorage;

    /**
     * 사용자 ID를 사용하여 특정 사용자의 프로필 정보를 조회합니다.
     * <p>
     * 이 메서드는 읽기 전용 트랜잭션(@Transactional(readOnly = true))으로 동작하여,
     * 데이터 변경 없이 조회 성능을 최적화합니다.
     *
     * @param userId 조회할 사용자의 고유 ID (UUID)
     * @return 조회된 사용자의 프로필 정보가 담긴 ProfileDto
     * @throws OtbooException 주어진 ID에 해당하는 사용자가 없거나,
     * 해당 사용자의 프로필이 존재하지 않을 경우 발생합니다.
     */
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

    /**
     * 특정 사용자의 프로필 정보 및 프로필 이미지를 수정합니다.
     *
     * 이 메서드는 @Transactional 환경에서 동작하며, JPA의 변경 감지(Dirty Checking) 기능을 통해
     * 프로필 정보 변경 시 데이터베이스에 자동으로 반영됩니다.
     * 새 프로필 이미지가 제공되면 기존 이미지를 S3에서 삭제하고 새 이미지를 업로드합니다.
     *
     * @param userId       수정할 사용자의 고유 ID (UUID)
     * @param request      수정할 프로필 정보가 담긴 DTO (@Valid를 통해 유효성 검사)
     * @param profileImage 새로 등록할 프로필 이미지 파일 (선택 사항)
     * @return 수정이 완료된 최신 프로필 정보가 담긴 ProfileDto
     * @throws OtbooException 사용자를 찾을 수 없을 때 발생하는 예외
     */
    @Override
    @Transactional
    public ProfileDto updateProfile(UUID userId, ProfileUpdateRequest request, MultipartFile profileImage) {
        log.info(SERVICE + "사용자 프로필 수정 시도 - 사용자 ID: {}, 사용자 이름: {}",
                userId, request.name());
        Profile existingProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new OtbooException(ErrorCode.PROFILE_NOT_FOUND));

        String oldImageUrl = existingProfile.getProfileImageUrl();
        String newImageUrl = oldImageUrl;

        if (profileImage != null && !profileImage.isEmpty()) {
            log.info(SERVICE + "사용자 프로필 이미지 최신화 - 사용자 ID: {}", userId);
            newImageUrl = s3ImageStorage.uploadImage(profileImage, "profile/");
            log.info(SERVICE + "사용자 프로필 업로드 완료. 업로드된 이미지: {}", newImageUrl);

            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                s3ImageStorage.deleteImage(oldImageUrl);
                log.info(SERVICE + "기존 프로필 이미지 삭제 완료. URL: {}", oldImageUrl);
            }
        }

        existingProfile.update(request, newImageUrl);
        log.info(SERVICE + "사용자 프로필 수정 완료 - 프로필 ID: {}", existingProfile.getId());

        return profileMapper.toDto(existingProfile);
    }
}
