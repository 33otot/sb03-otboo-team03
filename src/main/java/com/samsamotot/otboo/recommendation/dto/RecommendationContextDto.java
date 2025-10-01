package com.samsamotot.otboo.recommendation.dto;

import java.time.Month;
import lombok.Builder;

/**
 * ItemSelectorEngine이 점수를 계산하는 데 필요한
 * 외부 환경 정보(날씨, 시간 등)를 담는 DTO
 */
@Builder
public record RecommendationContextDto(
    double adjustedTemperature,
    boolean isRainingOrSnowing,
    Month currentMonth
) {

}
