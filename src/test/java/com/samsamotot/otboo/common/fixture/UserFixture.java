package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import java.util.UUID;

public class UserFixture {

    public static final String DEFAULT_USER_EMAIL = "testUser@test.com";
    public static final String DEFAULT_USER_NAME = "testUser";
    public static final String DEFAULT_USER_PASSWORD = "testPassword";
    public static final String DEFAULT_USER_IMAGE_URL = "http://example.com/profile.jpg";

    public static User createUser() {
        return User.builder()
            .email(DEFAULT_USER_EMAIL)
            .username(DEFAULT_USER_NAME)
            .password(DEFAULT_USER_PASSWORD)
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
