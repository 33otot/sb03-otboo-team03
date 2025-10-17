package com.samsamotot.otboo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.jwt.TokenInvalidationService;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.dto.UserLockRequest;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.service.impl.UserServiceImpl;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 계정 잠금 단위 테스트")
class UserServiceLockTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private TokenInvalidationService tokenInvalidationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private UserLockRequest lockRequest;
    private UserLockRequest unlockRequest;
    private UserDto lockedUserDto;
    private UserDto unlockedUserDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = UserFixture.createValidUser();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "role", Role.USER);
        ReflectionTestUtils.setField(user, "isLocked", false);

        lockRequest = new UserLockRequest(true);
        unlockRequest = new UserLockRequest(false);

        lockedUserDto = UserDto.builder()
            .id(userId)
            .createdAt(Instant.now())
            .email("user@example.com")
            .name("Test User")
            .role(Role.USER)
            .locked(true)
            .build();

        unlockedUserDto = UserDto.builder()
            .id(userId)
            .createdAt(Instant.now())
            .email("user@example.com")
            .name("Test User")
            .role(Role.USER)
            .locked(false)
            .build();
    }

    @Nested
    @DisplayName("계정 잠금 성공 테스트")
    class LockAccountSuccessTests {

        @Test
        @DisplayName("계정_잠금_성공")
        void 계정_잠금_성공() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(lockedUserDto);

        // when
        UserDto result = userService.updateUserLockStatus(userId, lockRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getLocked()).isTrue();
        verify(tokenInvalidationService).setUserInvalidAfter(eq(userId.toString()), any(Instant.class));
        }

        @Test
        @DisplayName("계정_잠금_해제_성공")
        void 계정_잠금_해제_성공() {
        // given
        ReflectionTestUtils.setField(user, "isLocked", true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(unlockedUserDto);

        // when
        UserDto result = userService.updateUserLockStatus(userId, unlockRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getLocked()).isFalse();
        // 잠금 해제 시에는 토큰 무효화가 호출되지 않아야 함
        }
    }

    @Nested
    @DisplayName("계정 잠금 실패 테스트")
    class LockAccountFailureTests {

        @Test
        @DisplayName("존재하지_않는_사용자_404_에러")
        void 존재하지_않는_사용자_404_에러() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUserLockStatus(userId, lockRequest))
            .isInstanceOf(OtbooException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("토큰 무효화 테스트")
    class TokenInvalidationTests {

        @Test
        @DisplayName("계정_잠금시_토큰_무효화_호출")
        void 계정_잠금시_토큰_무효화_호출() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(lockedUserDto);

        // when
        userService.updateUserLockStatus(userId, lockRequest);

        // then
        verify(tokenInvalidationService).setUserInvalidAfter(eq(userId.toString()), any(Instant.class));
        }

        @Test
        @DisplayName("계정_잠금_해제시_토큰_무효화_호출_안함")
        void 계정_잠금_해제시_토큰_무효화_호출_안함() {
        // given
        ReflectionTestUtils.setField(user, "isLocked", true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(unlockedUserDto);

        // when
        userService.updateUserLockStatus(userId, unlockRequest);

        // then
        // 토큰 무효화가 호출되지 않아야 함 (잠금 해제 시에는 토큰 무효화하지 않음)
        }
    }
}
