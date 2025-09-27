package com.samsamotot.otboo.recommendation.service;

import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추천(Recommendation) 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationServiceImpl implements RecommendationService {

    @Override
    public RecommendationDto recommendClothes(UUID userId, UUID weatherId) {
        return null;
    }
}
