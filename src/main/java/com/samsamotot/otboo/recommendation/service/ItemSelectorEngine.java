package com.samsamotot.otboo.recommendation.service;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.mapper.ClothesMapper;
import com.samsamotot.otboo.recommendation.dto.RecommendationContextDto;
import com.samsamotot.otboo.recommendation.dto.RecommendationResult;
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

    private static final class Scored {
        final Clothes c;
        final double score; // [0,1]
        final Style style;
        Scored(Clothes c, double score, Style style) { this.c = c; this.score = score; this.style = style; }
    }

    /**
     * 옷장 목록에서 추천 후보를 추려 각 카테고리별로 softmax 샘플링으로 추천합니다.
     * 추천 실패 시 랜덤 추천으로 대체합니다.
     *
     * @param clothes 사용자의 옷장에 있는 옷 목록
     * @param context 추천 컨텍스트(온도, 월, 강수 등)
     * @param rollCounter 샘플링용 시드값
     * @param cooldownIdMap 최근 추천된 옷(타입별) 맵
     * @return 추천된 OotdDto 리스트
     */
    public RecommendationResult createRecommendation(
        List<Clothes> clothes,
        RecommendationContextDto context,
        long rollCounter,
        Map<ClothesType, UUID> cooldownIdMap
    ) {
        boolean usedRandom = false;

        // null-safety
        final List<Clothes> safeClothes = (clothes == null) ? List.of() : clothes;
        final Map<ClothesType, UUID> safeCooldown =
            (cooldownIdMap == null) ? Map.of() : cooldownIdMap;

        // 전체 타입별 그룹화 (쿨다운 제외 X) - 랜덤 추천에서 사용
        Map<ClothesType, List<Clothes>> typeGroupsAll = safeClothes.stream()
            .collect(Collectors.groupingBy(Clothes::getType));

        // 최근 추천된 옷 제외 - 정상 추천 경로에서 사용
        List<Clothes> filtered = safeClothes.stream()
            .filter(c -> !Objects.equals(safeCooldown.get(c.getType()), c.getId()))
            .toList();

        Map<ClothesType, List<Clothes>> typeGroupsFiltered = filtered.stream()
            .collect(Collectors.groupingBy(Clothes::getType));

        // 점수 캐시 (중복 계산 방지)
        Map<UUID, Double> scoreCache = new HashMap<>();

        // TOP/BOTTOM 또는 DRESS 조합 우선 추천
        List<OotdDto> ootds = recommendTopBottomOrDress(typeGroupsFiltered, context, rollCounter, scoreCache);
        Style anchorStyle = findAnchorStyle(ootds, typeGroupsFiltered);

        // 나머지 타입 추천
        List<OotdDto> result = new ArrayList<>(ootds);
        result.addAll(recommendOthers(typeGroupsFiltered, context, rollCounter, anchorStyle, scoreCache));

        // 최종 결과가 비었으면 랜덤 폴백 수행
        if (result.isEmpty()) {
            log.debug(ENGINE + "최종 결과가 비어 랜덤 폴백 수행");
            result.addAll(fallbackRecommendTopBottomOrDress(typeGroupsAll, rollCounter));
            result.addAll(fallbackRecommendOthers(typeGroupsAll, rollCounter));

            usedRandom = !result.isEmpty();
            return new RecommendationResult(result, usedRandom);
        }

        // TOP/BOTTOM/DRESS가 없으면 랜덤 폴백 수행
        boolean hasCore = result.stream().anyMatch(dto ->
            dto.type() == ClothesType.TOP ||
                dto.type() == ClothesType.BOTTOM ||
                dto.type() == ClothesType.DRESS
        );
        if (!hasCore) {
            log.debug(ENGINE + "코어 아이템이 없어 코어 랜덤 폴백 수행");
            List<OotdDto> coreFallback = fallbackRecommendTopBottomOrDress(typeGroupsAll, rollCounter);

            if (!coreFallback.isEmpty()) {
                result.addAll(coreFallback);
                usedRandom = true;
            }
        }

        return new RecommendationResult(result, usedRandom);
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
     */
    private List<OotdDto> recommendTopBottomOrDress(
        Map<ClothesType, List<Clothes>> typeGroups,
        RecommendationContextDto context,
        long rollCounter,
        Map<UUID, Double> scoreCache
    ) {
        log.debug(ENGINE + "TOP/BOTTOM 또는 DRESS 추천 시작");
        List<OotdDto> result = new ArrayList<>();

        // 점수 한 번만 계산
        List<Scored> tops = buildScored(typeGroups.getOrDefault(ClothesType.TOP, List.of()), context, scoreCache);
        List<Scored> bottoms = buildScored(typeGroups.getOrDefault(ClothesType.BOTTOM, List.of()), context, scoreCache);
        List<Scored> dresses = buildScored(typeGroups.getOrDefault(ClothesType.DRESS, List.of()), context, scoreCache);

        // 임계값 이상
        List<Scored> topsOK    = filterAboveThreshold(tops);
        List<Scored> bottomsOK = filterAboveThreshold(bottoms);
        List<Scored> dressesOK = filterAboveThreshold(dresses);

        // 조합 우선: TOP/BOTTOM 있으면, TOP 고르고 BOTTOM에 보너스 가산 후 softmax
        if (!topsOK.isEmpty() && !bottomsOK.isEmpty()) {
            Clothes topPicked = softmaxPick(topsOK, mixSeed(rollCounter, ClothesType.TOP));
            Style anchor = extractStyle(topPicked);
            Clothes bottomPicked = softmaxPick(
                withHarmonyBonus(bottomsOK, anchor, STYLE_WEIGHT),
                mixSeed(rollCounter, ClothesType.BOTTOM)
            );

            if (topPicked != null && bottomPicked != null) {
                addDtoIfNotNull(result, topPicked);
                addDtoIfNotNull(result, bottomPicked);
                return result;
            }
        }

        // DRESS 단일
        if (!dressesOK.isEmpty()) {
            Clothes d = softmaxPick(dressesOK, mixSeed(rollCounter, ClothesType.DRESS));
            addDtoIfNotNull(result, d);
            return result;
        }

        // 단일 TOP 혹은 단일 BOTTOM
        if (!topsOK.isEmpty()) {
            addDtoIfNotNull(result, softmaxPick(topsOK, mixSeed(rollCounter, ClothesType.TOP)));
            return result;
        }
        if (!bottomsOK.isEmpty()) {
            addDtoIfNotNull(result, softmaxPick(bottomsOK, mixSeed(rollCounter, ClothesType.BOTTOM)));
            return result;
        }

        log.debug(ENGINE + "코어 점수 기반 후보 없음(임계값={})", scoreThreshold);
        return result;
    }

    /**
     * 점수 목록을 생성합니다.
     */
    private List<Scored> buildScored(List<Clothes> items, RecommendationContextDto ctx, Map<UUID, Double> cache) {
        if (items == null || items.isEmpty()) return List.of();
        List<Scored> out = new ArrayList<>(items.size());
        for (Clothes c : items) {
            double s = scoreOf(c, ctx, cache);
            out.add(new Scored(c, s, extractStyle(c)));
        }
        return out;
    }

    /**
     * 임계값 이상 점수만 필터링합니다.
     */
    private List<Scored> filterAboveThreshold(List<Scored> scored) {
        return scored.stream().filter(s -> s.score >= scoreThreshold).toList();
    }

    /**
     * 두 스타일 간 조화 점수를 계산해 보너스 점수를 가산합니다.
     */
    private List<Scored> withHarmonyBonus(List<Scored> scored, Style anchor, double weight) {
        if (anchor == null || scored.isEmpty()) return scored;
        List<Scored> out = new ArrayList<>(scored.size());
        for (Scored s : scored) {
            if (s.style == null) {
                out.add(s);
                continue;
            }
            double harmony = calculateHarmonyScore(anchor, s.style);
            double bonus = (harmony / 10.0) * weight;
            out.add(new Scored(s.c, Math.min(1.0, s.score + bonus), s.style));
        }
        return out;
    }

    /**
     * softmax 선택
     */
    private Clothes softmaxPick(List<Scored> scored, long seedSalt) {
        if (scored.isEmpty()) return null;
        List<Double> arr = scored.stream().map(s -> s.score).toList();
        int idx = softmaxSample(arr, seedSalt);
        return (idx >= 0 && idx < scored.size()) ? scored.get(idx).c : null;
    }

    /**
     * TOP/BOTTOM 또는 DRESS을 랜덤으로 추천합니다.
     * - TOP/BOTTOM 둘 다 있으면 한 벌 추천
     * - DRESS가 있으면 50% 확률로 DRESS, 아니면 TOP/BOTTOM 한 벌
     * - 둘 중 하나만 있으면 그것만 추천
     * - 모두 없으면 빈 리스트 반환
     */
    private List<OotdDto> fallbackRecommendTopBottomOrDress(
        Map<ClothesType, List<Clothes>> typeGroups,
        long rollCounter
    ) {
        log.debug(ENGINE + "랜덤 TOP/BOTTOM 또는 DRESS 추천 시작");

        List<OotdDto> result = new ArrayList<>();

        List<Clothes> tops = typeGroups.getOrDefault(ClothesType.TOP, List.of());
        List<Clothes> bottoms = typeGroups.getOrDefault(ClothesType.BOTTOM, List.of());
        List<Clothes> dresses = typeGroups.getOrDefault(ClothesType.DRESS, List.of());

        boolean hasPair  = !tops.isEmpty() && !bottoms.isEmpty();
        boolean hasDress = !dresses.isEmpty();

        if (hasPair && hasDress) {
            boolean pickDress = new SplittableRandom(mixSeed(rollCounter, "coreChoice")).nextInt(2) == 0;
            if (pickDress) {
                addDtoIfNotNull(result, getRandomCandidate(dresses, mixSeed(rollCounter, ClothesType.DRESS)));
                return result;
            } else {
                addDtoIfNotNull(result, getRandomCandidate(tops, mixSeed(rollCounter, ClothesType.TOP)));
                addDtoIfNotNull(result, getRandomCandidate(bottoms, mixSeed(rollCounter, ClothesType.BOTTOM)));
                return result;
            }
        }

        // TOP/BOTTOM 한 벌 가능
        if (hasPair) {
            addDtoIfNotNull(result, getRandomCandidate(tops, mixSeed(rollCounter, ClothesType.TOP)));
            addDtoIfNotNull(result, getRandomCandidate(bottoms, mixSeed(rollCounter, ClothesType.BOTTOM)));
            return result;
        }

        // DRESS 단일 가능
        if (hasDress) {
            addDtoIfNotNull(result, getRandomCandidate(dresses, mixSeed(rollCounter, ClothesType.DRESS)));
            return result;
        }

        // TOP만 또는 BOTTOM만 있는 경우
        if (!tops.isEmpty()) {
            addDtoIfNotNull(result, getRandomCandidate(tops, mixSeed(rollCounter, ClothesType.TOP)));
            return result;
        }
        if (!bottoms.isEmpty()) {
            addDtoIfNotNull(result, getRandomCandidate(bottoms, mixSeed(rollCounter, ClothesType.BOTTOM)));
            return result;
        }

        log.debug(ENGINE + "랜덤 폴백 대상(TOP/BOTTOM/DRESS)이 없습니다.");
        return result;
    }

    /**
     * TOP, BOTTOM, DRESS 외의 나머지 타입에서 임계값 이상 후보만 softmax로 선택합니다.
     * (임계값 이상 후보가 없으면 스킵되며, 최종 결과가 비었을 때 상위 레벨의 랜덤 폴백이 적용됩니다.)
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

        for (Map.Entry<ClothesType, List<Clothes>> e : typeGroups.entrySet()) {
            ClothesType type = e.getKey();
            if (type == ClothesType.TOP || type == ClothesType.BOTTOM || type == ClothesType.DRESS) continue;

            List<Scored> scored = buildScored(e.getValue(), context, scoreCache);
            List<Scored> ok = filterAboveThreshold(scored);
            ok = withHarmonyBonus(ok, anchorStyle, OTHER_STYLE_WEIGHT);

            if (ok.isEmpty()) {
                log.debug(ENGINE + "임계값 이상 후보 없음: type={}, threshold={}", type, scoreThreshold);
                continue;
            }
            Clothes pick = softmaxPick(ok, mixSeed(rollCounter, type));
            addDtoIfNotNull(result, pick);
        }
        return result;
    }

    /**
     * 나머지 타입을 랜덤으로 추천합니다.
     */
    private List<OotdDto> fallbackRecommendOthers(
        Map<ClothesType, List<Clothes>> typeGroups,
        long rollCounter
    ) {
        log.debug(ENGINE + "나머지 타입 랜덤 추천 시작");
        List<OotdDto> result = new ArrayList<>();

        for (Map.Entry<ClothesType, List<Clothes>> entry : typeGroups.entrySet()) {
            ClothesType type = entry.getKey();
            // 코어 타입 제외
            if (type == ClothesType.TOP || type == ClothesType.BOTTOM || type == ClothesType.DRESS) continue;

            List<Clothes> candidates = entry.getValue();
            if (candidates == null || candidates.isEmpty()) continue;

            Clothes picked = getRandomCandidate(candidates, mixSeed(rollCounter, type));
            addDtoIfNotNull(result, picked);
        }

        if (result.isEmpty()) {
            log.debug(ENGINE + "랜덤 폴백 대상(나머지 타입)이 없습니다.");
        }
        return result;
    }

    /**
     * 후보 중 랜덤으로 하나 반환
     */
    private Clothes getRandomCandidate(List<Clothes> candidates, long seed) {
        if (candidates == null || candidates.isEmpty()) return null;
        int idx = new SplittableRandom(seed).nextInt(candidates.size());
        return candidates.get(idx);
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
        UUID id = c.getId();
        if (id == null) {
            // 영속화 전/테스트 데이터 등 ID 없음: 캐시 미사용
            return calculateScore(c, ctx.adjustedTemperature(), ctx.currentMonth(), ctx.isRainingOrSnowing());
        }
        return cache.computeIfAbsent(id, k ->
            calculateScore(c, ctx.adjustedTemperature(), ctx.currentMonth(), ctx.isRainingOrSnowing())
        );
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
     * DTO 변환 및 null 체크 후 리스트에 추가
     */
    private void addDtoIfNotNull(List<OotdDto> result, Clothes clothes) {
        if (clothes == null) return;
        OotdDto dto = clothesMapper.toOotdDto(clothes);
        if (dto != null) {
            result.add(dto);
            log.debug(ENGINE + "추천 성공: id={}, type={}, attributes={}", clothes.getId(), clothes.getType(), clothes.getAttributes());
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
                        if (dto.clothesId() != null && Objects.equals(c.getId(), dto.clothesId())) {
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