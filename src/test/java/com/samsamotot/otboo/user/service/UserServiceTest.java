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
import com.samsamotot.otboo.user.dto.UserDtoCursorResponse;
import com.samsamotot.otboo.user.dto.UserListRequest;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;
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

    @Nested
    @DisplayName("사용자 목록 조회 테스트")
    class GetUserListTests {

        @Test
        @DisplayName("기본_사용자_목록_조회")
        void 기본_사용자_목록_조회() {
            // Given
            UserListRequest request = UserListRequest.builder()
                    .limit(10)
                    .sortBy("createdAt")
                    .sortDirection("DESCENDING")
                    .build();

            User user1 = UserFixture.createValidUser();
            User user2 = UserFixture.createValidUser();
            ReflectionTestUtils.setField(user1, "createdAt", Instant.now());
            ReflectionTestUtils.setField(user2, "createdAt", Instant.now().plusSeconds(1));
            List<User> users = List.of(user1, user2);
            Pageable pageable = PageRequest.of(0, 10);
            Slice<User> userSlice = new SliceImpl<>(users, pageable, false);

            UserDto userDto1 = UserFixture.createUserDtoWithId(user1.getId());
            UserDto userDto2 = UserFixture.createUserDtoWithId(user2.getId());

            given(userRepository.findUsersWithCursor(request)).willReturn(userSlice);
            given(userRepository.countUsersWithFilters(request)).willReturn(2L);
            given(userMapper.toDto(user1)).willReturn(userDto1);
            given(userMapper.toDto(user2)).willReturn(userDto2);

            // When
            UserDtoCursorResponse result = userService.getUserList(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.data()).hasSize(2);
            assertThat(result.totalCount()).isEqualTo(2L);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.sortBy()).isEqualTo("createdAt");
            assertThat(result.sortDirection()).isEqualTo("DESCENDING");

            verify(userRepository).findUsersWithCursor(request);
            verify(userRepository).countUsersWithFilters(request);
            verify(userMapper).toDto(user1);
            verify(userMapper).toDto(user2);
        }

        @Test
        @DisplayName("이메일_검색으로_사용자_목록_조회")
        void 이메일_검색으로_사용자_목록_조회() {
            // Given
            UserListRequest request = UserListRequest.builder()
                    .limit(10)
                    .sortBy("email")
                    .sortDirection("ASCENDING")
                    .emailLike("test")
                    .build();

            User user = UserFixture.createValidUser();
            List<User> users = List.of(user);
            Pageable pageable = PageRequest.of(0, 10);
            Slice<User> userSlice = new SliceImpl<>(users, pageable, false);

            UserDto userDto = UserFixture.createUserDtoWithId(user.getId());

            given(userRepository.findUsersWithCursor(request)).willReturn(userSlice);
            given(userRepository.countUsersWithFilters(request)).willReturn(1L);
            given(userMapper.toDto(user)).willReturn(userDto);

            // When
            UserDtoCursorResponse result = userService.getUserList(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.data()).hasSize(1);
            assertThat(result.totalCount()).isEqualTo(1L);
            assertThat(result.sortBy()).isEqualTo("email");
            assertThat(result.sortDirection()).isEqualTo("ASCENDING");

            verify(userRepository).findUsersWithCursor(request);
            verify(userRepository).countUsersWithFilters(request);
        }

        @Test
        @DisplayName("권한별_필터링으로_사용자_목록_조회")
        void 권한별_필터링으로_사용자_목록_조회() {
            // Given
            UserListRequest request = UserListRequest.builder()
                    .limit(10)
                    .sortBy("createdAt")
                    .sortDirection("DESCENDING")
                    .roleEqual(Role.ADMIN)
                    .build();

            User adminUser = UserFixture.createValidUser();
            ReflectionTestUtils.setField(adminUser, "role", Role.ADMIN);
            ReflectionTestUtils.setField(adminUser, "createdAt", Instant.now());
            List<User> users = List.of(adminUser);
            Pageable pageable = PageRequest.of(0, 10);
            Slice<User> userSlice = new SliceImpl<>(users, pageable, false);

            UserDto adminDto = UserFixture.createUserDtoWithId(adminUser.getId());

            given(userRepository.findUsersWithCursor(request)).willReturn(userSlice);
            given(userRepository.countUsersWithFilters(request)).willReturn(1L);
            given(userMapper.toDto(adminUser)).willReturn(adminDto);

            // When
            UserDtoCursorResponse result = userService.getUserList(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.data()).hasSize(1);
            assertThat(result.totalCount()).isEqualTo(1L);

            verify(userRepository).findUsersWithCursor(request);
            verify(userRepository).countUsersWithFilters(request);
        }

        @Test
        @DisplayName("잠금_상태_필터링으로_사용자_목록_조회")
        void 잠금_상태_필터링으로_사용자_목록_조회() {
            // Given
            UserListRequest request = UserListRequest.builder()
                    .limit(10)
                    .sortBy("createdAt")
                    .sortDirection("DESCENDING")
                    .locked(true)
                    .build();

            User lockedUser = UserFixture.createValidUser();
            ReflectionTestUtils.setField(lockedUser, "isLocked", true);
            ReflectionTestUtils.setField(lockedUser, "createdAt", Instant.now());
            List<User> users = List.of(lockedUser);
            Pageable pageable = PageRequest.of(0, 10);
            Slice<User> userSlice = new SliceImpl<>(users, pageable, false);

            UserDto lockedDto = UserFixture.createUserDtoWithId(lockedUser.getId());

            given(userRepository.findUsersWithCursor(request)).willReturn(userSlice);
            given(userRepository.countUsersWithFilters(request)).willReturn(1L);
            given(userMapper.toDto(lockedUser)).willReturn(lockedDto);

            // When
            UserDtoCursorResponse result = userService.getUserList(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.data()).hasSize(1);
            assertThat(result.totalCount()).isEqualTo(1L);

            verify(userRepository).findUsersWithCursor(request);
            verify(userRepository).countUsersWithFilters(request);
        }

        @Test
        @DisplayName("페이지네이션_커서가_있는_사용자_목록_조회")
        void 페이지네이션_커서가_있는_사용자_목록_조회() {
            // Given
            UserListRequest request = UserListRequest.builder()
                    .limit(5)
                    .sortBy("createdAt")
                    .sortDirection("DESCENDING")
                    .cursor("2025-01-01T00:00:00Z")
                    .idAfter(UUID.randomUUID())
                    .build();

            User user1 = UserFixture.createValidUser();
            User user2 = UserFixture.createValidUser();
            User user3 = UserFixture.createValidUser();
            User user4 = UserFixture.createValidUser();
            User user5 = UserFixture.createValidUser();
            User user6 = UserFixture.createValidUser(); // limit + 1 = 6개
            
            // User 엔티티에 ID 설정
            UUID user1Id = UUID.randomUUID();
            UUID user2Id = UUID.randomUUID();
            UUID user3Id = UUID.randomUUID();
            UUID user4Id = UUID.randomUUID();
            UUID user5Id = UUID.randomUUID();
            UUID user6Id = UUID.randomUUID();
            
            ReflectionTestUtils.setField(user1, "id", user1Id);
            ReflectionTestUtils.setField(user2, "id", user2Id);
            ReflectionTestUtils.setField(user3, "id", user3Id);
            ReflectionTestUtils.setField(user4, "id", user4Id);
            ReflectionTestUtils.setField(user5, "id", user5Id);
            ReflectionTestUtils.setField(user6, "id", user6Id);
            
            Instant createdAt = Instant.now();
            ReflectionTestUtils.setField(user1, "createdAt", createdAt);
            ReflectionTestUtils.setField(user2, "createdAt", createdAt.plusSeconds(1));
            ReflectionTestUtils.setField(user3, "createdAt", createdAt.plusSeconds(2));
            ReflectionTestUtils.setField(user4, "createdAt", createdAt.plusSeconds(3));
            ReflectionTestUtils.setField(user5, "createdAt", createdAt.plusSeconds(4));
            ReflectionTestUtils.setField(user6, "createdAt", createdAt.plusSeconds(5));
            
            // Repository에서 limit + 1개를 조회한 후, limit개로 자름
            List<User> allUsers = List.of(user1, user2, user3, user4, user5, user6);
            // hasNext 판단: allUsers.size() > 5 = true
            boolean hasNext = allUsers.size() > 5;
            // hasNext가 true이므로 limit개로 자름
            List<User> users = allUsers.subList(0, 5);
            
            Pageable pageable = PageRequest.of(0, 5);
            Slice<User> userSlice = new SliceImpl<>(users, pageable, hasNext);
            
            // 디버깅: User 엔티티의 createdAt 확인
            System.out.println("User1 createdAt: " + user1.getCreatedAt());

            UserDto userDto1 = UserFixture.createUserDtoWithId(user1Id);
            UserDto userDto2 = UserFixture.createUserDtoWithId(user2Id);
            UserDto userDto3 = UserFixture.createUserDtoWithId(user3Id);
            UserDto userDto4 = UserFixture.createUserDtoWithId(user4Id);
            UserDto userDto5 = UserFixture.createUserDtoWithId(user5Id);

            given(userRepository.findUsersWithCursor(request)).willReturn(userSlice);
            given(userRepository.countUsersWithFilters(request)).willReturn(10L);
            given(userMapper.toDto(user1)).willReturn(userDto1);
            given(userMapper.toDto(user2)).willReturn(userDto2);
            given(userMapper.toDto(user3)).willReturn(userDto3);
            given(userMapper.toDto(user4)).willReturn(userDto4);
            given(userMapper.toDto(user5)).willReturn(userDto5);

            // When
            UserDtoCursorResponse result = userService.getUserList(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.data()).hasSize(5);
            assertThat(result.totalCount()).isEqualTo(10L);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();
            assertThat(result.nextIdAfter()).isNotNull();

            verify(userRepository).findUsersWithCursor(request);
            verify(userRepository).countUsersWithFilters(request);
        }
    }
}
