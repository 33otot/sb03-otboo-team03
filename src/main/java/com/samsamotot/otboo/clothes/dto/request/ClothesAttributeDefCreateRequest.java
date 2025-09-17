package com.samsamotot.otboo.clothes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

@Builder
public record ClothesAttributeDefCreateRequest(
    @NotBlank(message = "속성 정의의 이름은 필수입니다.")
    String name,

    @NotEmpty(message = "옵션 리스트는 비어 있을 수 없습니다.")
    List<@NotBlank(message = "각 옵션 값은 공백일 수 없습니다.") String> selectableValues
) { }
