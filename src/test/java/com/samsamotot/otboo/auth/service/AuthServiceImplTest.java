package com.samsamotot.otboo.auth.service;

import com.samsamotot.otboo.auth.dto.LoginRequest;
import com.samsamotot.otboo.auth.service.impl.AuthServiceImpl;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.csrf.CsrfTokenService;
import com.samsamotot.otboo.common.security.jwt.JwtDto;
import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.entity.Provider;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private CsrfTokenService csrfTokenService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
        .email("test@example.com")
        .username("tester")
        .password("encoded-password")
        .provider(Provider.LOCAL)
        .providerId(null)
        .role(Role.USER)
        .isLocked(false)
        .temporaryPasswordExpiresAt((Instant) null)
        .build();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    @Test
    @DisplayName("로그인 성공 시 JwtDto 반환")
    void login_success() {
        // given
        LoginRequest request = LoginRequest.builder()
        .email("test@example.com")
        .password("plain-password")
        .build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("plain-password", "encoded-password")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(userId)).willReturn("access.jwt");
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn("refresh.jwt");
        given(jwtTokenProvider.getExpirationTime("access.jwt")).willReturn(3600L);
        given(userMapper.toDto(user)).willReturn(UserDto.builder().id(userId).email("test@example.com").name("tester").locked(false).build());

        // when
        JwtDto result = authService.login(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access.jwt");
        assertThat(result.getRefreshToken()).isEqualTo("refresh.jwt");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);
        assertThat(result.getUserDto()).isNotNull();
        assertThat(result.getUserDto().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("존재하지 않는 이메일 로그인 시 USER_NOT_FOUND 예외")
    void login_userNotFound() {
        // given
        LoginRequest request = LoginRequest.builder()
        .email("none@example.com")
        .password("pw")
        .build();

        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        // expect
        assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(OtbooException.class);
    }

    @Test
    @DisplayName("잠긴 계정 로그인 시 USER_LOCKED 예외")
    void login_userLocked() {
        // given
        User lockedUser = User.builder()
        .email("lock@example.com")
        .username("lock")
        .password("encoded")
        .provider(Provider.LOCAL)
        .providerId(null)
        .role(Role.USER)
        .isLocked(true)
        .temporaryPasswordExpiresAt((Instant) null)
        .build();
        ReflectionTestUtils.setField(lockedUser, "id", UUID.randomUUID());

        LoginRequest request = LoginRequest.builder()
        .email("lock@example.com")
        .password("pw")
        .build();

        given(userRepository.findByEmail("lock@example.com")).willReturn(Optional.of(lockedUser));

        // expect
        assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(OtbooException.class);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 EMAIL_OR_PASSWORD_MISMATCH 예외")
    void login_passwordMismatch() {
        // given
        LoginRequest request = LoginRequest.builder()
        .email("test@example.com")
        .password("wrong")
        .build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

        // expect
        assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(OtbooException.class);
    }

    @Test
    @DisplayName("로그아웃은 유효/무효 토큰 모두 예외 없이 통과")
    void logout_alwaysOk() {
        // given
        given(jwtTokenProvider.validateToken("any.jwt")).willReturn(true);

        // when/then (no exception)
        authService.logout("any.jwt");

        // invalid token path
        given(jwtTokenProvider.validateToken("bad.jwt")).willThrow(new RuntimeException("invalid"));
        authService.logout("bad.jwt");
    }
}


