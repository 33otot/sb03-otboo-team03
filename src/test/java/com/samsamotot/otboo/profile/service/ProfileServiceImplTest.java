package com.samsamotot.otboo.profile.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.ProfileFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.storage.S3ImageStorage;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.profile.entity.Gender;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.mapper.ProfileMapper;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.profile.service.impl.ProfileServiceImpl;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceImplTest {

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileMapper profileMapper;

    @Mock
    private S3ImageStorage s3ImageStorage;

    @Nested
    @DisplayName("유저 프로필 조회 테스트")
    class GetProfileTests {

        @Test
        void 프로필_조회하면_프로필_반환_성공() {
            // Given
            Profile profile = ProfileFixture.builder().build();
            UUID userId = profile.getUser().getId();
            ProfileDto profileDto = ProfileDto.builder()
                    .userId(userId)
                    .locationId(profile.getLocation().getId())
                    .name(profile.getName())
                    .gender(profile.getGender())
                    .birthDate(profile.getBirthDate())
                    .temperatureSensitivity(profile.getTemperatureSensitivity())
                    .profileImageUrl(profile.getProfileImageUrl())
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(profile.getUser()));
            given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
            given(profileMapper.toDto(profile)).willReturn(profileDto);

            // When
            ProfileDto result = profileService.getProfileByUserId(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.name()).isEqualTo(profile.getName());

            verify(userRepository).findById(userId);
            verify(profileRepository).findByUserId(userId);
            verify(profileMapper).toDto(profile);
        }

        @Test
        void 존재하지_않는_유저로_조회하면_예외() {
            // Given
            User user = UserFixture.createUser();
            UUID userId = user.getId();

            // 유저 조회 실패
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // When
            OtbooException exception = assertThrows(OtbooException.class,
                    () -> profileService.getProfileByUserId(userId));

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findById(userId);
        }

        @Test
        void 존재하지_않는_프로필로_조회하면_예외() {
            // Given
            User user = UserFixture.createUser();
            UUID userId = user.getId();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(profileRepository.findByUserId(userId)).willReturn(Optional.empty());

            // When
            OtbooException exception = assertThrows(OtbooException.class,
                    () -> profileService.getProfileByUserId(userId));

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_FOUND);

            verify(userRepository).findById(userId);
            verify(profileRepository).findByUserId(userId);
        }
    }

    @Nested
    @DisplayName("유저 프로필 수정 테스트")
    class UpdateProfileTests {

        @Test
        void 수정요청과_이미지가_주어지면_프로필_수정_성공() {
            // Given
            UUID userId = UUID.randomUUID();
            String profileImageUrl = "https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/new-image.png";

            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                    .name("수정된 이름")
                    .gender(Gender.MALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .location(LocationFixture.createLocation())
                    .temperatureSensitivity(5.0)
                    .build();
            MockMultipartFile profileImageFile = new MockMultipartFile(
                    "profileImageUrl",
                    "profile1.png",
                    MediaType.IMAGE_PNG_VALUE,
                    profileImageUrl.getBytes()
            );

            Profile existingProfile = ProfileFixture.perfectProfileWithNoImage();

            when(profileRepository.findByUserId(userId))
                    .thenReturn(Optional.of(existingProfile));

            when(s3ImageStorage.uploadImage(profileImageFile, "profile/"))
                    .thenReturn(profileImageUrl);

            // When
            ProfileDto resultDto = profileService.updateProfile(userId, request, profileImageFile);

            // Then
            assertThat(resultDto.name()).isEqualTo(request.name());
            assertThat(resultDto.profileImageUrl()).isEqualTo(profileImageUrl);
        }

        @Test
        void 수정요청만_주어지면_프로필_수정_성공() {
            // Given
            UUID userId = UUID.randomUUID();

            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                    .name("수정된 이름")
                    .gender(Gender.MALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .location(LocationFixture.createLocation())
                    .temperatureSensitivity(5.0)
                    .build();

            Profile existingProfile = ProfileFixture.perfectProfileWithImage("https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/new-image.png");

            when(profileRepository.findByUserId(userId))
                    .thenReturn(Optional.of(existingProfile));

            // When
            ProfileDto resultDto = profileService.updateProfile(userId, request, null);

            // Then
            assertThat(resultDto.name()).isEqualTo(request.name());
            assertThat(resultDto.profileImageUrl()).isEqualTo(existingProfile.getProfileImageUrl());
        }

        @Test
        void 존재하지_않는_유저의_프로필_수정하면_404_NOT_FOUND() {
            // Given
            UUID inValidUserId = UUID.randomUUID();

            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                    .name("수정된 이름")
                    .gender(Gender.MALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .location(LocationFixture.createLocation())
                    .temperatureSensitivity(5.0)
                    .build();

            when(profileRepository.findByUserId(inValidUserId))
                    .thenReturn(Optional.empty());

            // When
            // Then
            assertThat(() -> {
                profileService.updateProfile(inValidUserId, request, null);
            })
                    .isInstanceOf(OtbooException.class);
        }
    }
}
