package com.samsamotot.otboo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.security.jwt.TokenInvalidationService;
import com.samsamotot.otboo.user.dto.ChangePasswordRequest;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.user.service.impl.UserServiceImpl;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 비밀번호 변경 단위 테스트")
class UserServiceChangePasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenInvalidationService tokenInvalidationService;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private ChangePasswordRequest validRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = UserFixture.createValidUser();
        ReflectionTestUtils.setField(user, "id", userId);
        validRequest = ChangePasswordRequest.builder()
            .password("newPassword123")
            .build();
    }

    @Nested
    @DisplayName("비밀번호 변경 성공 테스트")
    class ChangePasswordSuccessTests {

        @Test
        @DisplayName("유효한_요청으로_비밀번호_변경_성공")
        void 유효한_요청으로_비밀번호_변경_성공() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.encode(validRequest.password())).willReturn("encodedNewPassword");
            doNothing().when(tokenInvalidationService).setUserInvalidAfter(any(String.class), any(Instant.class));

            // when
            userService.changePassword(userId, validRequest);

            // then
            verify(userRepository).findById(userId);
            verify(passwordEncoder).encode(validRequest.password());
            assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
            assertThat(user.getTemporaryPasswordExpiresAt()).isNull();
            verify(tokenInvalidationService).setUserInvalidAfter(eq(userId.toString()), any(Instant.class));
        }

        @Test
        @DisplayName("비밀번호_변경시_이전_토큰_무효화_서비스_호출")
        void 비밀번호_변경시_이전_토큰_무효화_서비스_호출() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.encode(validRequest.password())).willReturn("encodedNewPassword");
            doNothing().when(tokenInvalidationService).setUserInvalidAfter(any(String.class), any(Instant.class));

            // when
            userService.changePassword(userId, validRequest);

            // then
            verify(tokenInvalidationService).setUserInvalidAfter(eq(userId.toString()), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 실패 테스트")
    class ChangePasswordFailureTests {

        @Test
        @DisplayName("존재하지_않는_사용자_ID로_비밀번호_변경시_USER_NOT_FOUND_예외_발생")
        void 존재하지_않는_사용자_ID로_비밀번호_변경시_USER_NOT_FOUND_예외_발생() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.changePassword(userId, validRequest))
                .isInstanceOf(OtbooException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findById(userId);
            verify(passwordEncoder, never()).encode(any(String.class));
            verify(tokenInvalidationService, never()).setUserInvalidAfter(any(String.class), any(Instant.class));
        }
    }
}
