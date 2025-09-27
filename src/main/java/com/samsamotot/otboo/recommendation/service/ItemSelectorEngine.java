package com.samsamotot.otboo.recommendation.service;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.recommendation.dto.RecommendationContextDto;
import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemSelectorEngine {

    private ClothesRepository clothesRepository;

    public RecommendationDto createRecommendation(
        List<Clothes> clothes,
        RecommendationContextDto context,
        long rollCounter,
        Map<ClothesType, Long> cooldownIdMap
    ) {
        return null;
    }

    public double calculateScore(
        Clothes clothes,
        int temperature,
        String season,
        boolean isRainy
    ) {
        return 0.0;
    }

    public int softmaxSample(List<Double> scores, double seed) {
        return 0;
    }
}