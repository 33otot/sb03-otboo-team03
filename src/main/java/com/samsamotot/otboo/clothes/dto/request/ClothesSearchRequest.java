package com.samsamotot.otboo.clothes.dto.request;

import com.samsamotot.otboo.clothes.entity.ClothesType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ClothesSearchRequest(
    @NotNull(message = "옷장 소유자의 id는 필수입니다.")
    UUID ownerId,
    ClothesType typeEqual,
    String cursor,
    UUID idAfter,
    @NotNull @Positive @Max(50)
    Integer limit
) {

}