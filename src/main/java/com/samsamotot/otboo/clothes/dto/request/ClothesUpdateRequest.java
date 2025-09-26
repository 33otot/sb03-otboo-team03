package com.samsamotot.otboo.clothes.dto.request;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDto;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ClothesUpdateRequest(
    @NotBlank(message = "의상 이름은 필수입니다.")
    String name,

    @NotNull(message = "의상 타입은 필수입니다.")
    ClothesType type,

    List<ClothesAttributeDto> attributes
) {

}
