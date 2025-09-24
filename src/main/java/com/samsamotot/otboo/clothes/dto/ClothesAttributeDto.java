package com.samsamotot.otboo.clothes.dto;

import java.util.UUID;

public record ClothesAttributeDto(
    UUID definitionId,
    String value
) {

}
