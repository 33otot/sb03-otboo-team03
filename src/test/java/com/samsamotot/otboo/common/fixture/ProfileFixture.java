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
                .gender(gender)
                .build();
    }

    public static Profile birthProfile(LocalDate birtyDate) {
        return builder()
                .birthDate(birtyDate)
                .build();
    }

    public static Profile temperatureProfile(Double temperatureSensitivity) {
        return builder()
                .temperatureSensitivity(temperatureSensitivity)
                .build();
    }

    public static Profile imageProfile(String profileImageUrl) {
        return builder()
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public static Profile perfectProfileWithImage(
    ) {
        LocalDate now = LocalDate.now();
        return builder()
                .gender(Gender.MALE)
                .birthDate(now)
                .temperatureSensitivity(3.0)
                .profileImageUrl(null)
                .build();
    }
}
