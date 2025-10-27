package com.samsamotot.otboo.profile.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.ProfileFixture;
import com.samsamotot.otboo.common.storage.S3ImageStorage;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.profile.dto.NotificationSettingUpdateRequest;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.profile.entity.Gender;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.mapper.ProfileMapper;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.profile.service.impl.ProfileServiceImpl;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.repository.GridRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceImplTest {

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private GridRepository gridRepository;

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
            WeatherAPILocation weatherLocation = new WeatherAPILocation(
                    37.1234, 127.1234, 60, 127, List.of("서울특별시", "강남구")
            );
            ProfileDto profileDto = ProfileDto.builder()
                    .userId(userId)
                    .location(weatherLocation)
                    .name(profile.getName())
                    .gender(profile.getGender())
                    .birthDate(profile.getBirthDate())
                    .temperatureSensitivity(profile.getTemperatureSensitivity())
                    .profileImageUrl(profile.getProfileImageUrl())
                    .build();

            given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
            given(profileMapper.toDto(profile)).willReturn(profileDto);

            // When
            ProfileDto result = profileService.getProfileByUserId(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.name()).isEqualTo(profile.getName());

            verify(profileRepository).findByUserId(userId);
            verify(profileMapper).toDto(profile);
        }

        @Test
        void 존재하지_않는_유저로_조회하면_예외() {
            // Given
            UUID userId = UUID.randomUUID();
            given(profileRepository.findByUserId(userId)).willReturn(Optional.empty());

            // When
            // Then
            assertThatThrownBy(() -> profileService.getProfileByUserId(userId))
                    .isInstanceOf(OtbooException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_NOT_FOUND);
        }

        @Test
        void 존재하지_않는_프로필로_조회하면_예외() {
            // Given
            UUID userId = UUID.randomUUID();

            given(profileRepository.findByUserId(userId)).willReturn(Optional.empty());

            // When
            OtbooException exception = assertThrows(OtbooException.class,
                    () -> profileService.getProfileByUserId(userId));

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_FOUND);

            verify(profileRepository).findByUserId(userId);
        }
    }

    @Nested
    @DisplayName("유저 프로필 수정 테스트")
    class UpdateProfileTests {

        @Test
        void 이미지_있는_프로필_수정_성공하면_200_DTO() {
            // Given
            UUID userId = UUID.randomUUID();
            String oldImageUrl = "http://s3.com/old-image.png";
            String newImageUrl = "http://s3.com/new-image.png";

            WeatherAPILocation weatherLocation = new WeatherAPILocation(
                    37.1234, 127.1234, 60, 127, List.of("서울특별시", "강남구")
            );
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                    .name("수정된 이름")
                    .gender(Gender.MALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .location(weatherLocation)
                    .temperatureSensitivity(5.0)
                    .build();
            MockMultipartFile profileImageFile = new MockMultipartFile(
                    "profileImage", "profile.png", MediaType.IMAGE_PNG_VALUE, "new_image_data".getBytes()
            );

            Profile existingProfile = ProfileFixture.perfectProfileWithImage(oldImageUrl);
            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

            Grid grid = new Grid(weatherLocation.x(), weatherLocation.y());
            when(gridRepository.findByXAndY(weatherLocation.x(), weatherLocation.y())).thenReturn(Optional.of(grid));
            Location location = LocationFixture.createLocation(grid);
            when(locationRepository.findByLongitudeAndLatitude(weatherLocation.longitude(), weatherLocation.latitude())).thenReturn(Optional.of(location));

            when(s3ImageStorage.uploadImage(any(), anyString())).thenReturn(newImageUrl);

            given(profileMapper.toDto(any(Profile.class))).willAnswer(invocation -> {
                Profile profile = invocation.getArgument(0);
                return ProfileDto.builder()
                        .name(profile.getName())
                        .profileImageUrl(profile.getProfileImageUrl())
                        .build();
            });

            // When
            ProfileDto resultDto = profileService.updateProfile(userId, request, profileImageFile);

            // Then
            assertThat(resultDto.name()).isEqualTo(request.name());
            assertThat(resultDto.profileImageUrl()).isEqualTo(newImageUrl);

            verify(s3ImageStorage).uploadImage(profileImageFile, "profile/");
            verify(s3ImageStorage).deleteImage(oldImageUrl);
            verify(locationRepository).findByLongitudeAndLatitude(weatherLocation.longitude(), weatherLocation.latitude());
        }

        @Test
        void 이미지_없는_프로필_수정_성공하면_200_DTO() {
            // Given
            UUID userId = UUID.randomUUID();
            String existingImageUrl = "https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/new-image.png";

            WeatherAPILocation weatherLocation = new WeatherAPILocation(
                    37.5665, 126.9780, 55, 126, List.of("서울특별시", "종로구")
            );

            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                    .name("수정된 이름")
                    .gender(Gender.MALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .location(weatherLocation)
                    .temperatureSensitivity(5.0)
                    .build();

            Profile existingProfile = ProfileFixture.perfectProfileWithImage(existingImageUrl);

            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
            when(gridRepository.findByXAndY(anyInt(), anyInt())).thenReturn(Optional.empty());
            when(locationRepository.findByLongitudeAndLatitude(anyDouble(), anyDouble())).thenReturn(Optional.empty());

            when(gridRepository.save(any(Grid.class))).thenAnswer(invocation -> {
                Grid grid = invocation.getArgument(0);
                return new Grid(grid.getX(), grid.getY());
            });
            when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
                Location location = invocation.getArgument(0);
                return Location.builder().grid(location.getGrid()).build();
            });

            given(profileMapper.toDto(any(Profile.class))).willAnswer(invocation -> {
                Profile profile = invocation.getArgument(0);
                return ProfileDto.builder()
                        .name(profile.getName())
                        .profileImageUrl(profile.getProfileImageUrl())
                        .build();
            });

            // When
            ProfileDto resultDto = profileService.updateProfile(userId, request, null);

            // Then
            assertThat(resultDto.name()).isEqualTo(request.name());
            assertThat(resultDto.profileImageUrl()).isEqualTo(existingImageUrl);

            verify(s3ImageStorage, never()).uploadImage(any(), anyString());
            verify(gridRepository).save(any(Grid.class));
            verify(locationRepository).save(any(Location.class));
        }

        @Test
        void 존재하지_않는_유저의_프로필_수정하면_404_NOT_FOUND() {
            // Given
            UUID invalidUserId = UUID.randomUUID();

            WeatherAPILocation weatherLocation = new WeatherAPILocation(
                    37.5665, 126.9780, 55, 126, List.of("서울특별시", "종로구")
            );

            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                    .name("수정된 이름")
                    .gender(Gender.MALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .location(weatherLocation)
                    .temperatureSensitivity(5.0)
                    .build();

            when(profileRepository.findByUserId(invalidUserId))
                    .thenReturn(Optional.empty());

            // When
            // Then
            assertThatThrownBy(() -> profileService.updateProfile(invalidUserId, request, null))
                    .isInstanceOf(OtbooException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("날씨 알림 수신 여부 수정 테스트")
    class UpdateWeatherNotificationEnabled {

        @Test
        void 존재하는_사용자의_설정을_변경_성공() {

            // Given
            NotificationSettingUpdateRequest request = new NotificationSettingUpdateRequest(false);
            Profile existingProfile = ProfileFixture.profileWithWeatherEnabled(true);
            UUID userId = UUID.randomUUID();

            given(profileRepository.findByUserId(userId)).willReturn(Optional.of(existingProfile));

            // When
            profileService.updateNotificationEnabled(userId, request);

            // Then
            assertThat(existingProfile.isWeatherNotificationEnabled()).isFalse();
        }

        @Test
        void 존재하지_않는_사용자로_요청하면_OtbooException() {

            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            NotificationSettingUpdateRequest request = new NotificationSettingUpdateRequest(false);

            given(profileRepository.findByUserId(nonExistentUserId)).willReturn(Optional.empty());

            // When
            // Then
            assertThrows(OtbooException.class, () -> {
                profileService.updateNotificationEnabled(nonExistentUserId, request);
            });
        }
    }
}
