package com.samsamotot.otboo.recommendation.dto;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record RecommendationDto(
    UUID weatherId,
    UUID userId,
    List<OotdDto> clothes
) {

}
