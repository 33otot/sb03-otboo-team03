package com.samsamotot.otboo.profile.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.ProfileFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.profile.dto.ProfileDto;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
}
