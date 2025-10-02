package com.samsamotot.otboo.recommendation.service;

import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import java.util.UUID;

public interface RecommendationService {

    RecommendationDto recommendClothes(UUID userId, UUID weatherId);
}
