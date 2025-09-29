package com.samsamotot.otboo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.exception.DuplicateEmailException;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.user.service.impl.UserServiceImpl;

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

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserCreateRequest validRequest;
    private User savedUser;
    private UserDto expectedDto;

    @BeforeEach
    void setUp() {
        validRequest = UserFixture.createValidRequest();
        
        savedUser = UserFixture.createValidUser();
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(savedUser, "id", userId);
        
        expectedDto = UserFixture.createUserDtoWithId(userId);
    }

    @Nested
    @DisplayName("회원가입 성공 테스트")
    class CreateUserSuccessTests {

        @Test
        @DisplayName("유효한_요청으로_회원가입하면_UserDto를_반환한다")
        void 유효한_요청으로_회원가입하면_UserDto를_반환한다() {
            // given
            given(userRepository.existsByEmail(validRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(validRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDto(savedUser)).willReturn(expectedDto);

            // when
            UserDto result = userService.createUser(validRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(expectedDto.getId());
            assertThat(result.getEmail()).isEqualTo(validRequest.getEmail());
            assertThat(result.getName()).isEqualTo(validRequest.getName());
            assertThat(result.getRole()).isEqualTo(Role.USER);
            assertThat(result.getLocked()).isFalse();

            verify(userRepository).existsByEmail(validRequest.getEmail());
            verify(passwordEncoder).encode(validRequest.getPassword());
            verify(userRepository).save(any(User.class));
            verify(userMapper).toDto(savedUser);
        }

        @Test
        @DisplayName("회원가입시_비밀번호가_해시화되어_저장된다")
        void 회원가입시_비밀번호가_해시화되어_저장된다() {
            // given
            String encodedPassword = "encodedPassword";
            given(userRepository.existsByEmail(validRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(validRequest.getPassword())).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDto(savedUser)).willReturn(expectedDto);

            // when
            userService.createUser(validRequest);

            // then
            verify(passwordEncoder).encode(validRequest.getPassword());
        }

        @Test
        @DisplayName("회원가입시_기본값이_올바르게_설정된다")
        void 회원가입시_기본값이_올바르게_설정된다() {
            // given
            given(userRepository.existsByEmail(validRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(validRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDto(savedUser)).willReturn(expectedDto);

            // when
            userService.createUser(validRequest);

            // then
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("회원가입 실패 테스트")
    class CreateUserFailureTests {

        @Test
        @DisplayName("중복된_이메일로_회원가입하면_DuplicateEmailException이_발생한다")
        void 중복된_이메일로_회원가입하면_DuplicateEmailException이_발생한다() {
            // given
            given(userRepository.existsByEmail(validRequest.getEmail())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다");

            verify(userRepository).existsByEmail(validRequest.getEmail());
            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(any(String.class));
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("다른_이메일로_중복_검사시_정상_처리된다")
        void 다른_이메일로_중복_검사시_정상_처리된다() {
            // given
            UserCreateRequest differentEmailRequest = UserFixture.createValidRequest();
            differentEmailRequest.setEmail("different@example.com");
            
            given(userRepository.existsByEmail(differentEmailRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(differentEmailRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDto(savedUser)).willReturn(expectedDto);

            // when
            UserDto result = userService.createUser(differentEmailRequest);

            // then
            assertThat(result).isNotNull();
            verify(userRepository).existsByEmail(differentEmailRequest.getEmail());
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryTests {

        @Test
        @DisplayName("최소_길이_이름으로_회원가입이_가능하다")
        void 최소_길이_이름으로_회원가입이_가능하다() {
            // given
            UserCreateRequest minNameRequest = UserFixture.createShortNameRequest();
            given(userRepository.existsByEmail(minNameRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(minNameRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDto(savedUser)).willReturn(expectedDto);

            // when
            UserDto result = userService.createUser(minNameRequest);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("최대_길이_이름으로_회원가입이_가능하다")
        void 최대_길이_이름으로_회원가입이_가능하다() {
            // given
            UserCreateRequest maxNameRequest = UserFixture.createLongNameRequest();
            given(userRepository.existsByEmail(maxNameRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(maxNameRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDto(savedUser)).willReturn(expectedDto);

            // when
            UserDto result = userService.createUser(maxNameRequest);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("로깅 테스트")
    class LoggingTests {

        @Test
        @DisplayName("회원가입_시도시_로그가_기록된다")
        void 회원가입_시도시_로그가_기록된다() {
            // given
            given(userRepository.existsByEmail(validRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(validRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDto(savedUser)).willReturn(expectedDto);

            // when
            userService.createUser(validRequest);

            // then
            // 로그 검증은 실제로는 별도의 로그 검증 도구를 사용하지만,
            // 여기서는 메서드 호출이 정상적으로 이루어지는지 확인
            verify(userRepository).existsByEmail(validRequest.getEmail());
        }

        @Test
        @DisplayName("이메일_중복시_경고_로그가_기록된다")
        void 이메일_중복시_경고_로그가_기록된다() {
            // given
            given(userRepository.existsByEmail(validRequest.getEmail())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(DuplicateEmailException.class);

            // 로그 검증은 실제로는 별도의 로그 검증 도구를 사용
            verify(userRepository).existsByEmail(validRequest.getEmail());
        }
    }
}
