package com.samsamotot.otboo.clothes.dto.request;

import java.util.List;
import lombok.Builder;

@Builder
public record ClothesAttributeDefCreateRequest(
    String name,
    List<String> selectableValues
) { }
