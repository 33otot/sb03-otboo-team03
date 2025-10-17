package com.samsamotot.otboo.recommendation.dto;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import java.util.List;

public record RecommendationResult(
    List<OotdDto> items,
    boolean usedRandomFallback
) {}
