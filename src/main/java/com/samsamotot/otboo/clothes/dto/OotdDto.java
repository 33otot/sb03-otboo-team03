package com.samsamotot.otboo.clothes.dto;

import com.samsamotot.otboo.clothes.entity.ClothesType;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record OotdDto(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {

}
