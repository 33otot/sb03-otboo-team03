package com.samsamotot.otboo.profile.dto;

import com.samsamotot.otboo.profile.entity.Gender;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ProfileDto (
        UUID userId,
        UUID locationId,
        String name,
        Gender gender,
        LocalDate birthDate,
        Double temperatureSensitivity,
        String profileImageUrl
) {

}
