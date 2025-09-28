package com.samsamotot.otboo.recommendation.service;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.mapper.ClothesMapper;
import com.samsamotot.otboo.recommendation.dto.RecommendationContextDto;
import com.samsamotot.otboo.recommendation.type.Season;
import com.samsamotot.otboo.recommendation.type.Thickness;
import com.samsamotot.otboo.recommendation.type.Waterproof;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.regions.servicemetadata.ThinclientServiceMetadata;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemSelectorEngine {

    private static final String ENGINE = "[ItemSelectorEngine] ";

    // 점수 임계값
    private static final double SCORE_THRESHOLD = 0.4;

    // 의상 속성 이름 및 값 상수
    private static final String ATTR_THICKNESS = "두께";
    private static final String ATTR_WATERPROOF = "방수";
    private static final String ATTR_SEASON = "계절";

    // 점수 계산 가중치 (합이 1.0이 되도록 설정)
    private static final double TEMP_WEIGHT = 0.6;
    private static final double RAIN_WEIGHT = 0.2;
    private static final double SEASON_WEIGHT = 0.2;

    private final ClothesMapper clothesMapper;

    /**
     * 조건에 맞는 옷을 점수 기반으로 추려 softmax 샘플링으로 각 카테고리별로 하나씩 추천합니다.
     *
     * @param clothes 사용자의 옷장에 있는 옷 목록
     * @param context 추천 컨텍스트(온도, 월, 강수 등)
     * @param rollCounter 샘플링용 시드값
     * @param cooldownIdMap 최근 추천된 옷(타입별) 맵
     * @return 추천된 OotdDto 리스트
     */
    public List<OotdDto> createRecommendation(
        List<Clothes> clothes,
        RecommendationContextDto context,
        long rollCounter,
        Map<ClothesType, UUID> cooldownIdMap
    ) {
        List<OotdDto> result = new ArrayList<>();

        // 최근 추천된 옷(cooldown) 제외
        List<Clothes> filtered = clothes.stream()
            .filter(c -> !cooldownIdMap.containsKey(c.getType()) || !cooldownIdMap.get(c.getType()).equals(c.getId()))
            .toList();

        // 타입별 그룹화
        Map<ClothesType, List<Clothes>> typeGroups = filtered.stream()
            .collect(Collectors.groupingBy(Clothes::getType));

        // 타입별 각 옷에 대해 점수 계산 후 임계값 이상만 후보로 선정
        long seed = rollCounter;
        for (Map.Entry<ClothesType, List<Clothes>> entry : typeGroups.entrySet()) {
            List<Clothes> candidates = new ArrayList<>();
            List<Double> scores = new ArrayList<>();
            for (Clothes c : entry.getValue()) {
                double score = calculateScore(c, context.adjustedTemperature(),
                    context.currentMonth(), context.isRainingOrSnowing());
                if (score >= SCORE_THRESHOLD) {
                    candidates.add(c);
                    scores.add(score);
                }
            }

            // 후보가 있으면 softmax 샘플링으로 하나 선택
            if (!candidates.isEmpty()) {
                int selectedIdx = softmaxSample(scores, rollCounter);
                if (selectedIdx >= 0 && selectedIdx < candidates.size()) {
                    Clothes selected = candidates.get(selectedIdx);
                    // DTO 변환 및 null 체크
                    OotdDto ootdDto = clothesMapper.toOotdDto(selected);
                    if (ootdDto != null) {
                        result.add(ootdDto);
                        log.info(ENGINE + "추천 성공: id={}, type={}, score={}", selected.getId(), selected.getType(), scores.get(selectedIdx));
                    } else {
                        log.warn(ENGINE + "추천된 의상 DTO 변환 실패: id={}, type={}", selected.getId(), selected.getType());
                    }
                } else {
                    log.warn(ENGINE + "샘플링 결과가 후보 범위를 벗어남: selectedIdx={}, 후보 수={}", selectedIdx, candidates.size());
                }
            } else {
                log.info(ENGINE + "추천 가능한 의상이 없습니다. 임계값={}, 쿨다운 맵={}, 필터링 후 후보 수={}", SCORE_THRESHOLD, cooldownIdMap, filtered.size());
            }
        }

        return result;
    }

    /**
     * 옷의 속성과 추천 컨텍스트를 바탕으로 점수를 계산합니다.
     */
    public double calculateScore(
        Clothes clothes,
        double temperature,
        Month currentMonth,
        boolean isRainy
    ) {
        // 옷의 속성 리스트를 Map으로 변환
        Map<String, String> attributeMap = clothes.getAttributes().stream()
            .collect(Collectors.toMap(
                attr -> attr.getDefinition().getName(),
                attr -> attr.getValue(),
                (value1, value2) -> value1 // 중복 키 발생 시 첫 번째 값 사용
            ));

        Season currentSeason = Season.fromMonth(currentMonth);

        // 각 속성별 값 추출 및 기본값 설정
        Thickness thickness = Thickness.fromName(attributeMap.getOrDefault(ATTR_THICKNESS, Thickness.MEDIUM.getName()));
        boolean isWaterproof = Waterproof.TRUE.getName().equalsIgnoreCase(attributeMap.getOrDefault(ATTR_WATERPROOF, Waterproof.FALSE.getName()));
        Season clothesSeason = Season.fromName(attributeMap.getOrDefault(ATTR_SEASON, Season.SPRING.getName()));

        // 온도, 강수, 계절 적합도 계산
        double tempScore = calculateTempScore(thickness, temperature);
        double rainScore = isRainy ? (isWaterproof ? 1.0 : 0.5) : 1.0;
        double seasonScore = calculateSeasonScore(currentSeason, clothesSeason);

        // 계절 적합도가 0점이면 후보 제외 (여름-겨울)
        if (seasonScore == 0.0) {
            return 0.0;
        }

        // 최종 점수(가중치 반영)
        double finalScore = (tempScore * TEMP_WEIGHT) + (rainScore * RAIN_WEIGHT) + (seasonScore * SEASON_WEIGHT);
        return finalScore;
    }

    /**
     * 두께와 온도에 따라 온도 적합 점수를 계산합니다.
     */
    private double calculateTempScore(Thickness thickness, double temperature) {

        if (temperature > 22.0) {
            return switch (thickness) {
                case LIGHT -> 1.0;
                case MEDIUM -> 0.5;
                case HEAVY -> 0.0;
                default -> 0.5;
            };
        } else if (temperature >= 8.0 && temperature <= 22.0 ) {
            return switch (thickness) {
                case LIGHT -> 0.7;
                case MEDIUM -> 1.0;
                case HEAVY -> 0.3;
                default -> 0.7;
            };
        } else {
            return switch (thickness) {
                case LIGHT -> 0.0;
                case MEDIUM -> 0.7;
                case HEAVY -> 1.0;
                default -> 0.7;
            };
        }
    }

    /**
     * 현재 계절과 옷의 계절 속성에 따라 계절 적합 점수를 계산합니다.
     */
    private double calculateSeasonScore(Season currentSeason, Season clothesSeason) {
        if (currentSeason.equals(clothesSeason)) {
            return 1.0;
        }
        // 여름-겨울의 경우 0점
        if (Season.isCompletelyDifferent(currentSeason, clothesSeason)) {
            return 0.0;
        }
        return 0.5;
    }


    /**
     * softmax 확률 분포 기반으로 후보 중 하나를 샘플링합니다.
     *
     * @param scores 후보별 점수 리스트
     * @param seed 난수 시드값(rollCounter)
     * @return 선택된 인덱스(없으면 -1)
     */
    public int softmaxSample(List<Double> scores, long seed) {

        if (scores == null || scores.isEmpty()) return -1;

        // softmax 변환
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double[] exp = new double[scores.size()];
        double sum = 0.0;
        for (int i = 0; i < scores.size(); i++) {
            exp[i] = Math.exp(scores.get(i) - max);
            sum += exp[i];
        }

        // seed를 0~1 사이 값으로 변환
        double rand = new SplittableRandom(seed).nextDouble();

        // 누적 확률로 인덱스 선택
        double cum = 0.0;
        for (int i = 0; i < exp.length; i++) {
            cum += exp[i] / sum;
            if (rand < cum) return i;
        }

        return exp.length - 1;
    }
}