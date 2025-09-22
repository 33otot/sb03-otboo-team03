package com.samsamotot.otboo.clothes.dto.request;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDto;
import com.samsamotot.otboo.clothes.entity.ClothesAttribute;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ClothesCreateRequest(
    @NotNull(message = "유저 아이디는 필수입니다.")
    UUID ownerId,

    @NotBlank(message = "의상 이름은 필수입니다.")
    String name,

    @NotNull(message = "의상 타입은 필수입니다.")
    ClothesType type,

    List<ClothesAttributeDto> items
) {

}
