package com.samsamotot.otboo.clothes.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ClothesAttributeDefDto(
    UUID id,
    String name,
    List<String> selectableValues,
    Instant createdAt
) {

}
