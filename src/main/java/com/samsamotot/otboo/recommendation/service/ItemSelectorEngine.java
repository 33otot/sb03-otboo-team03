package com.samsamotot.otboo.recommendation.service;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.mapper.ClothesMapper;
import com.samsamotot.otboo.recommendation.dto.RecommendationContextDto;
import com.samsamotot.otboo.recommendation.type.RecommendationAttribute;
import com.samsamotot.otboo.recommendation.type.Season;
import com.samsamotot.otboo.recommendation.type.Style;
import com.samsamotot.otboo.recommendation.type.Thickness;
import com.samsamotot.otboo.recommendation.type.Waterproof;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ItemSelectorEngine {

    @Value("${recommendation.score-threshold:0.4}")
    private double scoreThreshold;

    // 점수 계산 가중치 (합이 1.0)
    private static final double TEMP_WEIGHT = 0.6;
    private static final double RAIN_WEIGHT = 0.2;
    private static final double SEASON_WEIGHT = 0.2;

    // 스타일 조화 가중치 (보너스 점수)
    private static final double STYLE_WEIGHT = 0.2;
    private static final double OTHER_STYLE_WEIGHT = 0.1;

    private static final String ENGINE = "[ItemSelectorEngine] ";

    private final ClothesMapper clothesMapper;

    /**
     * 옷장 목록에서 추천 후보를 추려 각 카테고리별로 softmax 샘플링으로 추천합니다.
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
        // 최근 추천된 옷 제외
        List<Clothes> filtered = clothes.stream()
            .filter(c -> !cooldownIdMap.containsKey(c.getType()) || !cooldownIdMap.get(c.getType()).equals(c.getId()))
            .toList();

        // 타입별 그룹화
        Map<ClothesType, List<Clothes>> typeGroups = filtered.stream()
            .collect(Collectors.groupingBy(Clothes::getType));

        // 점수 캐시 (중복 계산 방지)
        Map<UUID, Double> scoreCache = new HashMap<>();

        // TOP/BOTTOM 또는 DRESS 조합 우선 추천
        List<OotdDto> ootds = recommendTopBottomOrDress(typeGroups, context, rollCounter, scoreCache);
        Style anchorStyle = findAnchorStyle(ootds, typeGroups);

        // 나머지 타입 추천
        List<OotdDto> result = new ArrayList<>(ootds);
        result.addAll(recommendOthers(typeGroups, context, rollCounter, anchorStyle, scoreCache));
        return result;
    }

    /**
     * 옷의 속성과 추천 컨텍스트를 바탕으로 점수를 계산합니다.
     * 속성이 없거나 변환 오류 시 기본값으로 처리합니다.
     */
    public double calculateScore(
        Clothes clothes,
        double temperature,
        Month currentMonth,
        boolean isRainy
    ) {
        // 속성 리스트를 Map으로 변환 (변환 실패 시 null 키)
        Map<RecommendationAttribute, String> attributeMap =
            Optional.ofNullable(clothes.getAttributes()).orElseGet(List::of).stream()
                .map(attr -> {
                    if (attr == null || attr.getDefinition() == null) return null;
                    String name = attr.getDefinition().getName();
                    try {
                        return Map.entry(RecommendationAttribute.fromName(name), attr.getValue());
                    } catch (IllegalArgumentException e) {
                        log.warn(ENGINE + "의상 속성 변환 오류: name={}, err={}", name, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldV, newV) -> newV));
        log.debug(ENGINE + "의상 속성 맵: id={}, type={}, attributes={}", clothes.getId(), clothes.getType(), attributeMap);

        Season currentSeason = Season.fromMonth(currentMonth);

        // 각 속성별 값 추출
        Thickness thickness;
        try {
            thickness = Thickness.fromName(
                attributeMap.getOrDefault(RecommendationAttribute.THICKNESS, Thickness.MEDIUM.getName()));
        } catch (Exception ex) {
            log.warn(ENGINE + "두께 파싱 실패: value='{}', fallback=MEDIUM, id={}", attributeMap.get(RecommendationAttribute.THICKNESS), clothes.getId());
            thickness = Thickness.MEDIUM;
        }
        boolean isWaterproof = Waterproof.TRUE.getName().equalsIgnoreCase(
            attributeMap.getOrDefault(RecommendationAttribute.WATERPROOF, Waterproof.FALSE.getName()));
        Season clothesSeason;
        try {
            clothesSeason = Season.fromName(
                attributeMap.getOrDefault(RecommendationAttribute.SEASON, Season.SPRING.getName()));
        } catch (Exception ex) {
            log.warn(ENGINE + "계절 파싱 실패: value='{}', fallback=SPRING, id={}", attributeMap.get(RecommendationAttribute.SEASON), clothes.getId());
            clothesSeason = Season.SPRING;
        }

        // 온도, 강수, 계절 적합도 계산
        double tempScore = calculateTempScore(thickness, temperature);
        double rainScore = isRainy ? (isWaterproof ? 10.0 : 0.0) : 0.0;
        double seasonScore = calculateSeasonScore(currentSeason, clothesSeason);

        // 계절 적합도가 0점이면 후보 제외 (여름-겨울)
        if (seasonScore == 0.0) {
            return 0.0;
        }

        // 최종 점수(가중치 반영)
        double finalScore = ((tempScore * TEMP_WEIGHT) + (rainScore * RAIN_WEIGHT) + (seasonScore * SEASON_WEIGHT)) / 10.0;
        log.debug(ENGINE + "최종 점수 계산: tempScore={}, rainScore={}, seasonScore={}, finalScore={}",
            tempScore, rainScore, seasonScore, finalScore);
        return finalScore;
    }

    /**
     * softmax 확률 분포 기반으로 샘플링합니다.
     * (점수 분포가 극단적이면 경고 로그, 모든 점수 동일하면 균등 샘플링)
     *
     * @param scores 후보별 점수 리스트
     * @param seed 난수 시드값(rollCounter)
     * @return 선택된 인덱스(없으면 -1)
     */
    public int softmaxSample(List<Double> scores, long seed) {

        if (scores == null || scores.isEmpty()) return -1;

        // 모든 점수가 동일할 때는 균등 샘플링
        if (scores.stream().distinct().count() == 1) {
            log.debug(ENGINE + "모든 후보 점수가 동일합니다. 후보 수={}", scores.size());
            return new SplittableRandom(seed).nextInt(scores.size());
        }

        // softmax 변환
        double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        if (max - min > 0.8) {
            log.warn(ENGINE + "softmax 점수 분포가 극단적입니다. min={}, max={}", min, max);
        }
        double[] exp = new double[scores.size()];
        double sum = 0.0;
        for (int i = 0; i < scores.size(); i++) {
            exp[i] = Math.exp(scores.get(i) - max);
            sum += exp[i];
        }

        double rand = new SplittableRandom(seed).nextDouble();
        double cum = 0.0;
        for (int i = 0; i < exp.length; i++) {
            cum += exp[i] / sum;
            if (rand < cum) {
                log.debug(ENGINE + "softmaxSample: selectedIdx={}, scores={}", i, scores);
                return i;
            }
        }
        log.debug(ENGINE + "softmaxSample: selectedIdx={}, scores={}", exp.length - 1, scores);
        return exp.length - 1;
    }

    /**
     * TOP/BOTTOM 조합 또는 DRESS을 추천합니다.
     * (조합이 안되면 단일 아이템 추천)
     */
    private List<OotdDto> recommendTopBottomOrDress(
        Map<ClothesType, List<Clothes>> typeGroups,
        RecommendationContextDto context,
        long rollCounter,
        Map<UUID, Double> scoreCache
    ) {
        log.debug(ENGINE + "TOP/BOTTOM 또는 DRESS 추천 시작");
        List<OotdDto> result = new ArrayList<>();

        // 각 타입별 후보 필터링 및 점수 계산
        List<Clothes> dressCandidates = filterByScore(typeGroups.getOrDefault(ClothesType.DRESS, List.of()), context, scoreCache);
        List<Double> dressScores = calculateScores(dressCandidates, context, scoreCache);

        List<Clothes> topCandidates = filterByScore(typeGroups.getOrDefault(ClothesType.TOP, List.of()), context, scoreCache);
        List<Double> topScores = calculateScores(topCandidates, context, scoreCache);

        List<Clothes> bottomCandidates = filterByScore(typeGroups.getOrDefault(ClothesType.BOTTOM, List.of()), context, scoreCache);

        // TOP/BOTTOM 조합 추천
        if (!topCandidates.isEmpty() && !bottomCandidates.isEmpty()) {
            int selectedTopIdx = softmaxSample(topScores, mixSeed(rollCounter, ClothesType.TOP));
            Clothes selectedTop = getCandidate(topCandidates, selectedTopIdx);
            Style topStyle = extractStyle(selectedTop);

            // BOTTOM 후보별 스타일 조화 점수 반영
            List<Double> bottomScores = new ArrayList<>();
            for (Clothes c : bottomCandidates) {
                double base = scoreOf(c, context, scoreCache);
                double harmony = calculateHarmonyScore(topStyle, extractStyle(c));
                double score = Math.min(1.0, base + (harmony * STYLE_WEIGHT) / 10.0);
                bottomScores.add(score);
            }
            int selectedBottomIdx = softmaxSample(bottomScores, mixSeed(rollCounter, ClothesType.BOTTOM));
            Clothes selectedBottom = getCandidate(bottomCandidates, selectedBottomIdx);

            boolean hasTopBottom = selectedTop != null && selectedBottom != null;

            double topBottomSelectedAvg = hasTopBottom
                ? (topScores.get(selectedTopIdx) + bottomScores.get(selectedBottomIdx)) / 2.0
                : 0.0;

            int selectedDressIdx = softmaxSample(dressScores, mixSeed(rollCounter, ClothesType.DRESS));
            double selectedDressScore = (selectedDressIdx >= 0 && selectedDressIdx < dressScores.size())
                ? dressScores.get(selectedDressIdx)
                : 0.0;

            // DRESS가 더 적합하면 DRESS 추천, 아니면 TOP/BOTTOM 조합 추천
            if (selectedDressScore >= topBottomSelectedAvg && !dressCandidates.isEmpty()) {
                Clothes selectedDress = getCandidate(dressCandidates, selectedDressIdx);
                addDtoIfNotNull(result, selectedDress);
            } else if (hasTopBottom) {
                addDtoIfNotNull(result, selectedTop);
                addDtoIfNotNull(result, selectedBottom);
            } else {
                log.info(ENGINE + "추천 가능한 TOP/BOTTOM 조합이 없습니다.");
            }
            return result;
        }

        // DRESS 단일 추천
        if (!dressCandidates.isEmpty()) {
            int selectedDressIdx = softmaxSample(dressScores, mixSeed(rollCounter, ClothesType.DRESS));
            Clothes selectedDress = getCandidate(dressCandidates, selectedDressIdx);
            addDtoIfNotNull(result, selectedDress);
            return result;
        }

        // TOP만 있을 때 단일 추천
        if (!topCandidates.isEmpty()) {
            int selectedTopIdx = softmaxSample(topScores, mixSeed(rollCounter, ClothesType.TOP));
            Clothes selectedTop = getCandidate(topCandidates, selectedTopIdx);
            addDtoIfNotNull(result, selectedTop);
            return result;
        }

        // BOTTOM만 있을 때 단일 추천
        List<Double> bottomScores = calculateScores(bottomCandidates, context, scoreCache);
        if (!bottomCandidates.isEmpty()) {
            int selectedBottomIdx = softmaxSample(bottomScores, mixSeed(rollCounter, ClothesType.BOTTOM));
            Clothes selectedBottom = getCandidate(bottomCandidates, selectedBottomIdx);
            addDtoIfNotNull(result, selectedBottom);
            return result;
        }

        log.info(ENGINE + "추천 가능한 TOP/BOTTOM/DRESS 조합이 없습니다.");
        return result;
    }

    /**
     * TOP, BOTTOM, DRESS 외의 나머지 타입을 추천합니다.
     */
    private List<OotdDto> recommendOthers(
        Map<ClothesType, List<Clothes>> typeGroups,
        RecommendationContextDto context,
        long rollCounter,
        Style anchorStyle,
        Map<UUID, Double> scoreCache
    ) {
        log.debug(ENGINE + "나머지 타입 추천 시작");
        List<OotdDto> result = new ArrayList<>();
        for (Map.Entry<ClothesType, List<Clothes>> entry : typeGroups.entrySet()) {
            ClothesType type = entry.getKey();
            // TOP/BOTTOM/DRESS는 제외
            if (type == ClothesType.TOP || type == ClothesType.BOTTOM || type == ClothesType.DRESS)
                continue;

            List<Clothes> candidates = filterByScore(entry.getValue(), context, scoreCache);
            List<Double> scores = new ArrayList<>(candidates.size());

            for (Clothes c : candidates) {
                double base = scoreOf(c, context, scoreCache);
                double harmony = (anchorStyle != null) ? calculateHarmonyScore(anchorStyle, extractStyle(c)) : 0.0;
                double score = Math.min(1.0, base + (harmony / 10.0) * OTHER_STYLE_WEIGHT);
                scores.add(score);
            }

            if (candidates.isEmpty()) {
                log.info(ENGINE + "추천 가능한 의상이 없습니다. 임계값={}, type={}", scoreThreshold, type);
                continue;
            }

            int selectedIdx = softmaxSample(scores, mixSeed(rollCounter, type));
            Clothes selected = getCandidate(candidates, selectedIdx);
            addDtoIfNotNull(result, selected);
        }
        return result;
    }

    /**
     * 임계값 이상 후보 필터링
     */
    private List<Clothes> filterByScore(List<Clothes> clothes, RecommendationContextDto context, Map<UUID, Double> scoreCache) {
        List<Clothes> filtered = clothes.stream()
            .filter(c -> scoreOf(c, context, scoreCache) >= scoreThreshold)
            .toList();
        log.debug(ENGINE + "filterByScore: before={}, after={}, threshold={}", clothes.size(), filtered.size(), scoreThreshold);
        return filtered;
    }

    /**
     * 후보별 점수 리스트 반환
     */
    private List<Double> calculateScores(List<Clothes> clothes, RecommendationContextDto context, Map<UUID, Double> scoreCache) {
        List<Double> scores = clothes.stream()
            .map(c -> scoreOf(c, context, scoreCache))
            .toList();
        log.debug(ENGINE + "calculateScores: scores={}", scores);
        return scores;
    }

    /**
     * 옷의 스타일 속성 값을 추출합니다.
     */
    private Style extractStyle(Clothes clothes) {
        if (clothes == null || clothes.getAttributes() == null) return null;
        return clothes.getAttributes().stream()
            .filter(a -> a != null && a.getDefinition() != null
                && a.getDefinition().getName() != null
                && a.getDefinition().getName().equalsIgnoreCase(RecommendationAttribute.STYLE.getName()))
            .map(a -> {
                try { return Style.fromName(a.getValue()); }
                catch (Exception ignored) { return null; }
            })
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
    }

    /**
     * 캐시된 점수가 없으면 계산 후 캐시에 저장합니다.
     */
    private double scoreOf(Clothes c, RecommendationContextDto ctx, Map<UUID, Double> cache) {
        if (!cache.containsKey(c.getId())) {
            double score = calculateScore(c, ctx.adjustedTemperature(), ctx.currentMonth(), ctx.isRainingOrSnowing());
            log.debug(ENGINE + "scoreOf: cache miss for id={}, calculated score={}", c.getId(), score);
            cache.put(c.getId(), score);
        }
        return cache.get(c.getId());
    }

    /**
     * 두께와 온도에 따라 온도 적합 점수를 계산합니다.
     */
    private double calculateTempScore(Thickness thickness, double temperature) {
        double score;
        if (temperature > 22.0) {
            score = switch (thickness) {
                case LIGHT -> 10.0;
                case MEDIUM -> 5.0;
                case HEAVY -> 0.0;
                default -> 5.0;
            };
        } else if (temperature >= 8.0 && temperature <= 22.0 ) {
            score = switch (thickness) {
                case LIGHT -> 7.0;
                case MEDIUM -> 10.0;
                case HEAVY -> 3.0;
                default -> 7.0;
            };
        } else {
            score = switch (thickness) {
                case LIGHT -> 0.0;
                case MEDIUM -> 7.0;
                case HEAVY -> 10.0;
                default -> 7.0;
            };
        }
        log.debug(ENGINE + "온도 적합도 계산: thickness={}, temperature={}, score={}", thickness, temperature, score);
        return score;
    }

    /**
     * 현재 계절과 옷의 계절 속성에 따라 계절 적합 점수를 계산합니다.
     */
    private double calculateSeasonScore(Season currentSeason, Season clothesSeason) {
        double score;
        if (currentSeason.equals(clothesSeason)) {
            score = 10.0;
        } else if (Season.isCompletelyDifferent(currentSeason, clothesSeason)) {
            score = 0.0;
        } else {
            score = 5.0;
        }
        log.debug(ENGINE + "계절 적합도 계산: currentSeason={}, clothesSeason={}, score={}", currentSeason, clothesSeason, score);
        return score;
    }

    /**
     * 스타일 조화 점수를 계산합니다. (같으면 10점, 다르면 0점, 정보 없으면 5점)
     */
    private double calculateHarmonyScore(Style style1, Style style2) {
        if (style1 == null || style2 == null) return 5.0;
        if (style1 == style2) return 10.0;
        if (style1.isSimilar(style2)) return 7.0;
        return 0.0;
    }

    /**
     * 후보 중 softmax 인덱스에 해당하는 옷 반환
     */
    private Clothes getCandidate(List<Clothes> candidates, int idx) {
        return (idx >= 0 && idx < candidates.size()) ? candidates.get(idx) : null;
    }

    /**
     * DTO 변환 및 null 체크 후 리스트에 추가
     */
    private void addDtoIfNotNull(List<OotdDto> result, Clothes clothes) {
        if (clothes == null) return;
        OotdDto dto = clothesMapper.toOotdDto(clothes);
        if (dto != null) {
            result.add(dto);
            log.info(ENGINE + "추천 성공: id={}, type={}, attributes={}", clothes.getId(), clothes.getType(), clothes.getAttributes());
        } else {
            log.error(ENGINE + "추천된 의상 DTO 변환 실패: id={}, type={}", clothes.getId(), clothes.getType());
        }
    }

    /**
     * TOP 또는 DRESS 중 먼저 나오는 아이템의 스타일을 기준점으로 삼습니다.
     */
    private Style findAnchorStyle(List<OotdDto> ootds, Map<ClothesType, List<Clothes>> typeGroups) {
        for (OotdDto dto : ootds) {
            ClothesType type = dto.type();
            if (type == ClothesType.DRESS || type == ClothesType.TOP) {
                List<Clothes> group = typeGroups.get(type);
                if (group != null) {
                    for (Clothes c : group) {
                        if (c.getId().equals(dto.clothesId())) {
                            return extractStyle(c);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 시드와 솔트를 혼합하여 새로운 시드 생성
     */
    private long mixSeed(long seed, Object salt) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h ^= (salt == null ? 0 : salt.hashCode());
        h ^= (h >>> 33); h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33); h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }
}