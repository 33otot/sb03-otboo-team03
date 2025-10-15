package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.entity.Provider;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import java.time.Instant;
import java.util.UUID;

/**
 * 사용자 테스트 픽스처
 */
public class UserFixture {

    public static final String DEFAULT_USER_EMAIL = "testUser@test.com";
    public static final String DEFAULT_USER_NAME = "testUser";
    public static final String DEFAULT_USER_PASSWORD = "testPassword";
    public static final String DEFAULT_USER_IMAGE_URL = "http://example.com/profile.jpg";
    public static final String VALID_EMAIL = "test@example.com";
    public static final String VALID_NAME = "테스트사용자";
    public static final String VALID_PASSWORD = "Test123!@#";
    public static final String INVALID_EMAIL = "invalid-email";
    public static final String EMPTY_STRING = "";
    public static final String SHORT_NAME = "a";
    public static final String LONG_NAME = "a".repeat(51);
    public static final String SHORT_PASSWORD = "Test1!";
    public static final String INVALID_PASSWORD = "12345678";

    public static User createValidUser() {
        return User.builder()
            .email(VALID_EMAIL)
            .username(VALID_NAME)
            .password("encodedPassword")
            .provider(Provider.LOCAL)
            .providerId(null)
            .role(Role.USER)
            .isLocked(false)
            .build();
    }

    public static User createUserWithEmail(String email) {
        return User.builder()
            .email(email)
            .username(VALID_NAME)
            .password("encodedPassword")
            .provider(Provider.LOCAL)
            .providerId(null)
            .role(Role.USER)
            .isLocked(false)
            .build();
    }

    public static User createUserWithUsername(String username) {
        return User.builder()
            .email(VALID_EMAIL)
            .username(username)
            .password("encodedPassword")
            .provider(Provider.LOCAL)
            .providerId(null)
            .role(Role.USER)
            .isLocked(false)
            .build();
    }

    public static User createAdminUser() {
        return User.builder()
            .email("admin@example.com")
            .username("관리자")
            .password("encodedPassword")
            .provider(Provider.LOCAL)
            .providerId(null)
            .role(Role.ADMIN)
            .isLocked(false)
            .build();
    }

    public static User createLockedUser() {
        return User.builder()
            .email("locked@example.com")
            .username("잠긴사용자")
            .password("encodedPassword")
            .provider(Provider.LOCAL)
            .providerId(null)
            .role(Role.USER)
            .isLocked(true)
            .build();
    }

    public static User createUser() {
        return User.builder()
            .email(DEFAULT_USER_EMAIL)
            .username(DEFAULT_USER_NAME)
            .password(DEFAULT_USER_PASSWORD)
            .provider(Provider.LOCAL)
            .providerId(null)
            .role(Role.USER)
            .isLocked(false)
            .build();
    }

    public static User createUser(String email, String username, Role role, boolean locked) {
        return User.builder()
            .email(email)
            .username(username)
            .password("encodedPassword")
            .provider(Provider.LOCAL)
            .providerId(null)
            .role(role)
            .isLocked(locked)
            .build();
    }

    // ========== DTO 픽스처 ==========

    public static UserCreateRequest createValidRequest() {
        return UserCreateRequest.builder()
            .name(VALID_NAME)
            .email(VALID_EMAIL)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createValidUserCreateRequest() {
        return UserCreateRequest.builder()
            .name(VALID_NAME)
            .email(VALID_EMAIL)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createUserCreateRequestWithEmail(String email) {
        return UserCreateRequest.builder()
            .name(VALID_NAME)
            .email(email)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createUserCreateRequestWithName(String name) {
        return UserCreateRequest.builder()
            .name(name)
            .email(VALID_EMAIL)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createInvalidNameRequest() {
        return UserCreateRequest.builder()
            .name(EMPTY_STRING)
            .email(VALID_EMAIL)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createShortNameRequest() {
        return UserCreateRequest.builder()
            .name(SHORT_NAME)
            .email(VALID_EMAIL)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createLongNameRequest() {
        return UserCreateRequest.builder()
            .name(LONG_NAME)
            .email(VALID_EMAIL)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createInvalidEmailRequest() {
        return UserCreateRequest.builder()
            .name(VALID_NAME)
            .email(INVALID_EMAIL)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createEmptyEmailRequest() {
        return UserCreateRequest.builder()
            .name(VALID_NAME)
            .email(EMPTY_STRING)
            .password(VALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createInvalidPasswordRequest() {
        return UserCreateRequest.builder()
            .name(VALID_NAME)
            .email(VALID_EMAIL)
            .password(INVALID_PASSWORD)
            .build();
    }

    public static UserCreateRequest createShortPasswordRequest() {
        return UserCreateRequest.builder()
            .name(VALID_NAME)
            .email(VALID_EMAIL)
            .password(SHORT_PASSWORD)
            .build();
    }

    public static UserCreateRequest createEmptyPasswordRequest() {
        return UserCreateRequest.builder()
            .name(VALID_NAME)
            .email(VALID_EMAIL)
            .password(EMPTY_STRING)
            .build();
    }

    public static UserDto createValidUserDto() {
        return UserDto.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .email(VALID_EMAIL)
            .name(VALID_NAME)
            .role(Role.USER)
            .locked(false)
            .build();
    }

    public static UserDto createUserDtoWithId(UUID id) {
        return UserDto.builder()
            .id(id)
            .createdAt(Instant.now())
            .email(VALID_EMAIL)
            .name(VALID_NAME)
            .role(Role.USER)
            .locked(false)
            .build();
    }

    public static UserDto createUserDtoWithEmail(String email) {
        return UserDto.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .email(email)
            .name(VALID_NAME)
            .role(Role.USER)
            .locked(false)
            .build();
    }

    public static UserDto createUserDtoWithName(String name) {
        return UserDto.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .email(VALID_EMAIL)
            .name(name)
            .role(Role.USER)
            .locked(false)
            .build();
    }

    public static AuthorDto createAuthorDto(User user) {
        return AuthorDto.builder()
            .userId(user.getId())
            .name(user.getUsername())
            .profileImageUrl(DEFAULT_USER_IMAGE_URL)
            .build();
    }
}
