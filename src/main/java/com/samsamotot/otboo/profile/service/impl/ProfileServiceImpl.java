package com.samsamotot.otboo.profile.service.impl;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.storage.S3ImageStorage;
import com.samsamotot.otboo.common.util.CacheNames;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.NotificationSettingUpdateRequest;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.mapper.ProfileMapper;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.repository.GridRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.profile.service.impl
 * FileName     : ProfileServiceImpl
 * Author       : HuInDoL
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final String SERVICE_NAME = "[ProfileServiceImpl] ";

    private final ProfileRepository profileRepository;
    private final LocationRepository locationRepository;
    private final ProfileMapper profileMapper;
    private final S3ImageStorage s3ImageStorage;
    private final GridRepository gridRepository;

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
    @Cacheable(value = CacheNames.PROFILE, key = "#userId")
    public ProfileDto getProfileByUserId(UUID userId) {
        log.info(SERVICE_NAME + "사용자 프로필 조회 시도 - 사용자 ID: {}", userId);

        Profile userProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new OtbooException(ErrorCode.PROFILE_NOT_FOUND));

        User user = userProfile.getUser();
        if (user == null) { // Profile과 User의 연결이 끊어진 예외적인 경우 방어
            throw new OtbooException(ErrorCode.USER_NOT_FOUND);
        }
        log.info(SERVICE_NAME + "사용자 프로필 조회 성공 - 프로필 ID: {}", userProfile.getId());

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
    @CacheEvict(value = CacheNames.PROFILE, key = "#userId")
    public ProfileDto updateProfile(UUID userId, ProfileUpdateRequest request, MultipartFile profileImage) {
        log.info(SERVICE_NAME + "사용자 프로필 수정 시도 - 사용자 ID: {}, 사용자 이름: {}",
                userId, request.name());
        Profile existingProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new OtbooException(ErrorCode.PROFILE_NOT_FOUND));

        // 1. 이미지 처리
        String oldImageUrl = existingProfile.getProfileImageUrl();
        String newImageUrl = processProfileImage(profileImage, oldImageUrl);

        // 2. 위치 정보 처리
        Location location = findOrCreateLocation(request.location(), existingProfile.getLocation());

        // 3. [저장] 프로필 업데이트
        existingProfile.update(request, location, newImageUrl);
        log.info(SERVICE_NAME + "사용자 프로필 수정 완료 - 프로필 ID: {}", existingProfile.getId());

        return profileMapper.toDto(existingProfile);
    }

    /**
     * 요청 DTO에 포함된 위치 정보를 바탕으로 Location 엔티티를 조회하거나 새로 생성합니다.
     * @param weatherLocation 클라이언트로부터 받은 위치 정보 DTO
     * @param existingLocation weatherLocation이 null일 경우 반환될 기본 위치 엔티티
     * @return 영속 상태의 Location 엔티티
     */
    private Location findOrCreateLocation(WeatherAPILocation weatherLocation, Location existingLocation) {
        if (weatherLocation == null) {
            return existingLocation;
        }
            Grid grid = gridRepository.findByXAndY(weatherLocation.x(), weatherLocation.y())
                    .orElseGet(() -> {
                        Grid newGrid = Grid.builder()
                                .x(weatherLocation.x())
                                .y(weatherLocation.y())
                                .build();
                        try {
                            return gridRepository.save(newGrid);
                        } catch (DataIntegrityViolationException e) {
                            return gridRepository.findByXAndY(weatherLocation.x(), weatherLocation.y())
                                    .orElseThrow(() -> new OtbooException(ErrorCode.NOT_FOUND_GRID));
                        }
                    });
            return locationRepository.findByLongitudeAndLatitude(weatherLocation.longitude(), weatherLocation.latitude())
                    .orElseGet(() -> {
                        Location newLocation = Location.builder()
                                .longitude(weatherLocation.longitude())
                                .latitude(weatherLocation.latitude())
                                .grid(grid)
                                .locationNames(weatherLocation.locationNames())
                                .build();
                        try {
                            return locationRepository.save(newLocation);
                        } catch (DataIntegrityViolationException e) {
                            return locationRepository.findByLongitudeAndLatitude(weatherLocation.longitude(), weatherLocation.latitude())
                                    .orElseThrow(() -> new OtbooException(ErrorCode.NOT_FOUND_LOCATION));
                        }
                    });
    }

    private String processProfileImage(MultipartFile profileImage, String oldImageUrl) {
        if (profileImage == null || profileImage.isEmpty()) {
            log.info(SERVICE_NAME + "기존 프로필 이미지 변경 없음 - oldImageUrl: {}", oldImageUrl);
            return oldImageUrl;
        }

        log.info(SERVICE_NAME + "사용자 프로필 이미지 최신화...");
        String newImageUrl = s3ImageStorage.uploadImage(profileImage, "profile/");
        log.info(SERVICE_NAME + "사용자 프로필 업로드 완료. URL: {}", newImageUrl);

        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            s3ImageStorage.deleteImage(oldImageUrl);
            log.info(SERVICE_NAME + "기존 프로필 이미지 삭제 완료. URL: {}", oldImageUrl);
        }

        return newImageUrl;
    }

    /**
     * 사용자의 날씨 알림 수신 여부 설정을 업데이트합니다.
     * <p>
     * 요청 DTO에 포함된 weatherNotificationEnabled 값을 기반으로
     * 대상 사용자의 프로필 설정을 변경합니다.
     *
     * @param userId  설정을 변경할 사용자의 ID
     * @param request 변경할 설정값이 담긴 DTO. weatherNotificationEnabled 필드를 사용합니다.
     * @throws OtbooException 사용자의 프로필을 찾을 수 없을 경우 PROFILE_NOT_FOUND 에러를 발생시킵니다.
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.PROFILE, key = "#userId")
    public void updateNotificationEnabled(UUID userId, NotificationSettingUpdateRequest request) {
        if (request == null) {
            throw new OtbooException(ErrorCode.INVALID_REQUEST);
        }
        log.info(SERVICE_NAME + "유저 프로필 날씨 알림 수신 여부 수정 시작 - WeatherNotificationEnabled: {}", request.weatherNotificationEnabled());

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new OtbooException(ErrorCode.PROFILE_NOT_FOUND));

        profile.setWeatherNotificationEnabled(request.weatherNotificationEnabled());

        log.info(SERVICE_NAME + "유저 프로필 날씨 알림 수신 여부 수정 완료");
    }
}
