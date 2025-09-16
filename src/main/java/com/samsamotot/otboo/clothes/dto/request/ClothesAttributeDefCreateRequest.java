package com.samsamotot.otboo.clothes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
public record ClothesAttributeDefCreateRequest(
    @NotBlank(message = "속성 정의의 이름은 필수입니다.")
    String name,

    @NotNull(message = "옵션 리스트는 null일 수 없습니다.")
    @Size(min = 1, message = "최소 1개 이상의 옵션이 필요합니다.")
    List<String> selectableValues
) { }
