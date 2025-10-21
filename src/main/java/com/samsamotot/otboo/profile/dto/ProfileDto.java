package com.samsamotot.otboo.profile.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.samsamotot.otboo.profile.entity.Gender;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ProfileDto (
        UUID userId,
        WeatherAPILocation location,
        String name,
        Gender gender,

        LocalDate birthDate,

        Double temperatureSensitivity,
        String profileImageUrl,
        boolean weatherNotificationEnabled
) {

}
