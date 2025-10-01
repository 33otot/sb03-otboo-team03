package com.samsamotot.otboo.profile.dto;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.profile.entity.Gender;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ProfileUpdateRequest(

        @NotBlank @Size(min = 2, max = 10)
        String name,

        Gender gender,

        LocalDate birthDate,

        Location location,

        @DecimalMin(value = "1.0")
        @DecimalMax(value = "5.0")
        @Digits(integer = 1, fraction = 0)
        Double temperatureSensitivity
) { }
