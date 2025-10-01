package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.profile.entity.Gender;
import com.samsamotot.otboo.profile.entity.Profile;

import java.time.LocalDate;

public class ProfileFixture {

    public static Profile.ProfileBuilder builder() {
        return Profile.builder()
                .user(UserFixture.createUser())
                .location(LocationFixture.createLocation())
                .name(UserFixture.VALID_NAME);
    }

    public static Profile genderProfile(Gender gender) {
        return builder()
                .user(UserFixture.createUser())
                .location(LocationFixture.createLocation())
                .gender(gender)
                .build();
    }

    public static Profile birthProfile(LocalDate birthDate) {
        return builder()
                .user(UserFixture.createUser())
                .location(LocationFixture.createLocation())
                .birthDate(birthDate)
                .build();
    }

    public static Profile temperatureProfile(Double temperatureSensitivity) {
        return builder()
                .user(UserFixture.createUser())
                .location(LocationFixture.createLocation())
                .temperatureSensitivity(temperatureSensitivity)
                .build();
    }

    public static Profile imageProfile(String profileImageUrl) {
        return builder()
                .user(UserFixture.createUser())
                .location(LocationFixture.createLocation())
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public static Profile perfectProfileWithNoImage(
    ) {
        LocalDate now = LocalDate.now();
        return builder()
                .user(UserFixture.createUser())
                .location(LocationFixture.createLocation())
                .gender(Gender.MALE)
                .birthDate(now)
                .temperatureSensitivity(3.0)
                .profileImageUrl(null)
                .build();
    }

    public static Profile perfectProfileWithImage(String profileImageUrl
    ) {
        LocalDate now = LocalDate.now();
        return builder()
                .user(UserFixture.createUser())
                .location(LocationFixture.createLocation())
                .gender(Gender.MALE)
                .birthDate(now)
                .temperatureSensitivity(3.0)
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
