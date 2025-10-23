package com.samsamotot.otboo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesAttribute;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.mapper.ClothesMapper;
import com.samsamotot.otboo.recommendation.dto.RecommendationContextDto;
import com.samsamotot.otboo.recommendation.dto.RecommendationResult;
import com.samsamotot.otboo.recommendation.type.Style;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemSelectorEngine 단위 테스트")
public class ItemSelectorEngineTest {

    @Mock
    private ClothesMapper clothesMapper;

    @Spy
    @InjectMocks
    private ItemSelectorEngine itemSelectorEngine;

    ClothesAttributeDef thickness;
    ClothesAttributeDef season;
    ClothesAttributeDef waterproof;
    ClothesAttributeDef style;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(itemSelectorEngine, "scoreThreshold", 0.4);

        thickness = ClothesAttributeDef.createClothesAttributeDef("두께", List.of("얇음", "보통", "두꺼움"));
        season = ClothesAttributeDef.createClothesAttributeDef("계절", List.of("봄", "여름", "가을", "겨울"));
        waterproof = ClothesAttributeDef.createClothesAttributeDef("방수", List.of("가능", "불가능"));
        style = ClothesAttributeDef.createClothesAttributeDef("스타일", List.of("캐주얼", "포멀", "스포티", "시크", "빈티지", "클래식", "미니멀"));
    }

    @Test
    void 더운_날_가벼운_옷은_두꺼운_옷보다_높은_점수를_받는다() {

        // given
        int hotTemperature = 30;

        ClothesAttribute lightAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("얇음")
            .build();

        ClothesAttribute heavyAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("두꺼움")
            .build();

        Clothes lightClothes = Clothes.builder()
            .name("가벼운 상의")
            .type(ClothesType.TOP)
            .attributes(List.of(lightAttr))
            .build();

        Clothes heavyClothes = Clothes.builder()
            .name("두꺼운 상의")
            .type(ClothesType.TOP)
            .attributes(List.of(heavyAttr))
            .build();

        // when
        double lightScore = itemSelectorEngine.calculateScore(lightClothes, hotTemperature, Month.JUNE, false);
        double heavyScore = itemSelectorEngine.calculateScore(heavyClothes, hotTemperature, Month.JUNE, false);

        // then
        assertThat(lightScore).isGreaterThan(heavyScore);
    }

    @Test
    void 추운_날_두꺼운_옷은_가벼운_옷보다_높은_점수를_받는다() {

        // given
        int coldTemperature = 0;

        ClothesAttribute lightAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("얇음")
            .build();

        ClothesAttribute heavyAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("두꺼움")
            .build();

        Clothes lightClothes = Clothes.builder()
            .name("가벼운 상의")
            .type(ClothesType.TOP)
            .attributes(List.of(lightAttr))
            .build();

        Clothes heavyClothes = Clothes.builder()
            .name("두꺼운 상의")
            .type(ClothesType.TOP)
            .attributes(List.of(heavyAttr))
            .build();

        // when
        double lightScore = itemSelectorEngine.calculateScore(lightClothes, coldTemperature, Month.FEBRUARY, false);
        double heavyScore = itemSelectorEngine.calculateScore(heavyClothes, coldTemperature, Month.FEBRUARY, false);

        // then
        assertThat(heavyScore).isGreaterThan(lightScore);
    }

    @Test
    void 온화한_날_MID_두께의_옷이_가장_높은_점수를_받는다() {

        // given
        int mildTemperature = 15;

        ClothesAttribute lightAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("얇음")
            .build();

        ClothesAttribute midAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("보통")
            .build();

        ClothesAttribute heavyAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("두꺼움")
            .build();

        Clothes lightClothes = Clothes.builder()
            .name("가벼운 상의")
            .type(ClothesType.TOP)
            .attributes(List.of(lightAttr))
            .build();

        Clothes midClothes = Clothes.builder()
            .name("중간 상의")
            .type(ClothesType.TOP)
            .attributes(List.of(midAttr))
            .build();

        Clothes heavyClothes = Clothes.builder()
            .name("두꺼운 상의")
            .type(ClothesType.TOP)
            .attributes(List.of(heavyAttr))
            .build();

        // when
        double lightScore = itemSelectorEngine.calculateScore(lightClothes, mildTemperature,
            Month.OCTOBER, false);
        double midScore = itemSelectorEngine.calculateScore(midClothes, mildTemperature, Month.OCTOBER, false);
        double heavyScore = itemSelectorEngine.calculateScore(heavyClothes, mildTemperature, Month.OCTOBER, false);

        // then
        assertThat(midScore).isGreaterThan(heavyScore);
        assertThat(midScore).isGreaterThan(lightScore);
    }

    @Test
    void 계절_일치_옷은_추가_점수를_받는다() {

        // given
        Month currentMonth = Month.JUNE;

        ClothesAttribute summerSeasonAttr = ClothesAttribute.builder()
            .definition(season)
            .value("여름")
            .build();

        ClothesAttribute winterSeasonAttr = ClothesAttribute.builder()
            .definition(season)
            .value("겨울")
            .build();

        Clothes summerClothes = Clothes.builder()
            .name("여름 옷")
            .type(ClothesType.TOP)
            .attributes(List.of(summerSeasonAttr))
            .build();

        Clothes winterClothes = Clothes.builder()
            .name("겨울 옷")
            .type(ClothesType.TOP)
            .attributes(List.of(winterSeasonAttr))
            .build();

        // when
        double summerScore = itemSelectorEngine.calculateScore(summerClothes, 30, currentMonth, false);
        double winterScore = itemSelectorEngine.calculateScore(winterClothes, 30, currentMonth, false);

        // then
        assertThat(summerScore).isGreaterThan(winterScore);
    }

    @Test
    void 계절_완전_불일치시_점수_0_및_추천_제외된다() {

        // given
        ClothesAttribute summerAttr = ClothesAttribute.builder()
            .definition(season)
            .value("여름")
            .build();
        Clothes clothes = Clothes.builder()
            .name("여름 옷")
            .type(ClothesType.TOP)
            .attributes(List.of(summerAttr))
            .build();

        // when
        double score = itemSelectorEngine.calculateScore(clothes, 0, Month.JANUARY, false);

        // then
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void 비오는_날_방수_가능_옷은_추가_점수를_받는다() {

        // given
        boolean isRaining = true;

        ClothesAttribute waterproofAttr = ClothesAttribute.builder()
            .definition(waterproof)
            .value("가능")
            .build();

        ClothesAttribute nonWaterproofAttr = ClothesAttribute.builder()
            .definition(waterproof)
            .value("불가능")
            .build();

        Clothes waterproofClothes = Clothes.builder()
            .name("방수 옷")
            .type(ClothesType.TOP)
            .attributes(List.of(waterproofAttr))
            .build();

        Clothes nonWaterproofClothes = Clothes.builder()
            .name("비방수 옷")
            .type(ClothesType.TOP)
            .attributes(List.of(nonWaterproofAttr))
            .build();

        // when
        double waterproofScore = itemSelectorEngine.calculateScore(waterproofClothes, 20, Month.APRIL, isRaining);
        double nonWaterproofScore = itemSelectorEngine.calculateScore(nonWaterproofClothes, 20, Month.APRIL, isRaining);

        // then
        assertThat(waterproofScore).isGreaterThan(nonWaterproofScore);
    }

    @Test
    void 비오는_날_방수_불가능_옷은_추가_점수를_받지_않는다() {

        // given
        boolean isRaining = false;

        ClothesAttribute waterproofAttr = ClothesAttribute.builder()
            .definition(waterproof)
            .value("가능")
            .build();

        ClothesAttribute nonWaterproofAttr = ClothesAttribute.builder()
            .definition(waterproof)
            .value("불가능")
            .build();

        Clothes waterproofClothes = Clothes.builder()
            .name("방수 옷")
            .type(ClothesType.TOP)
            .attributes(List.of(waterproofAttr))
            .build();

        Clothes nonWaterproofClothes = Clothes.builder()
            .name("비방수 옷")
            .type(ClothesType.TOP)
            .attributes(List.of(nonWaterproofAttr))
            .build();

        // when
        double waterproofScore = itemSelectorEngine.calculateScore(waterproofClothes, 20, Month.APRIL, isRaining);
        double nonWaterproofScore = itemSelectorEngine.calculateScore(nonWaterproofClothes, 20, Month.APRIL, isRaining);

        // then
        assertThat(waterproofScore).isEqualTo(nonWaterproofScore);
    }

    @Test
    void 스타일_정보_없으면_조화_점수는_5점이다() {

        // given
        Style style1 = null;
        Style style2 = null;

        // when
        double score = ReflectionTestUtils.invokeMethod(itemSelectorEngine, "calculateHarmonyScore", style1, style2);

        // then
        assertThat(score).isEqualTo(5.0);
    }

    @Test
    void 스타일_같으면_조화_점수는_10점이다() {

        // given
        Style style1 = Style.CASUAL;
        Style style2 = Style.CASUAL;

        // when
        double score = ReflectionTestUtils.invokeMethod(itemSelectorEngine, "calculateHarmonyScore", style1, style2);

        // then
        assertThat(score).isEqualTo(10.0);
    }

    @Test
    void 스타일_유사하면_조화_점수는_7점이다() {

        // given
        Style style1 = Style.CASUAL;
        Style style2 = Style.MINIMAL;

        // when
        double score = ReflectionTestUtils.invokeMethod(itemSelectorEngine, "calculateHarmonyScore", style1, style2);

        // then
        assertThat(score).isEqualTo(7.0);
    }

    @Test
    void 스타일_다르면_조화_점수는_0점이다() {

        // given
        Style style1 = Style.CASUAL;
        Style style2 = Style.FORMAL;

        // when
        double score = ReflectionTestUtils.invokeMethod(itemSelectorEngine, "calculateHarmonyScore", style1, style2);

        // then
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void 고정된_시드로_Softmax_샘플링시_항상_같은_결과를_반환한다() {

        // given
        List<Double> scores = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        long fixedSeed = 100L;

        // when
        int firstSample = itemSelectorEngine.softmaxSample(scores, fixedSeed);
        int secondSample = itemSelectorEngine.softmaxSample(scores, fixedSeed);
        int thirdSample = itemSelectorEngine.softmaxSample(scores, fixedSeed);

        // then
        assertThat(firstSample).isEqualTo(secondSample);
        assertThat(secondSample).isEqualTo(thirdSample);
    }

    @Test
    void 점수가_임계값_미만인_아이템은_후보군에서_제외된다() {

        // given
        double temperature = 20.0;
        Month month = Month.APRIL;
        boolean isRainy = false;

        Clothes highScoreClothes = Clothes.builder()
            .name("추천될 옷")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();
        ReflectionTestUtils.setField(highScoreClothes, "id", UUID.randomUUID());

        Clothes lowScoreClothes = Clothes.builder()
            .name("제외될 옷")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();
        ReflectionTestUtils.setField(lowScoreClothes, "id", UUID.randomUUID());

        List<Clothes> clothesList = List.of(highScoreClothes, lowScoreClothes);
        RecommendationContextDto context = RecommendationContextDto.builder()
            .adjustedTemperature(temperature)
            .currentMonth(month)
            .isRainingOrSnowing(isRainy)
            .build();

        doReturn(0.5).when(itemSelectorEngine).calculateScore(highScoreClothes, temperature, month, isRainy);
        doReturn(0.3).when(itemSelectorEngine).calculateScore(lowScoreClothes, temperature, month, isRainy);
        doReturn(OotdDto.builder().name("추천될 옷").build())
            .when(clothesMapper).toOotdDto(any(Clothes.class));

        // when
        RecommendationResult rr = itemSelectorEngine.createRecommendation(
            clothesList, context, 0L, Map.of()
        );
        List<OotdDto> result = rr.items();

        // then
        assertThat(result)
            .extracting("name")
            .contains("추천될 옷")
            .doesNotContain("제외될 옷");
    }

    @Test
    void 카테고리_내_모든_아이템이_임계값_미만이면_랜덤_추천이_발생한다() {

        // given
        double temperature = 20.0;
        Month month = Month.APRIL;
        boolean isRainy = false;

        Clothes lowScoreClothes1 = Clothes.builder()
            .name("제외될 옷1")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();
        ReflectionTestUtils.setField(lowScoreClothes1, "id", UUID.randomUUID());

        Clothes lowScoreClothes2 = Clothes.builder()
            .name("제외될 옷2")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();
        ReflectionTestUtils.setField(lowScoreClothes2, "id", UUID.randomUUID());

        List<Clothes> clothesList = List.of(lowScoreClothes1, lowScoreClothes2);
        RecommendationContextDto context = RecommendationContextDto.builder()
            .adjustedTemperature(temperature)
            .currentMonth(month)
            .isRainingOrSnowing(isRainy)
            .build();

        doReturn(0.2).when(itemSelectorEngine).calculateScore(lowScoreClothes1, temperature, month, isRainy);
        doReturn(0.1).when(itemSelectorEngine).calculateScore(lowScoreClothes2, temperature, month, isRainy);

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder()
                    .clothesId(c.getId())
                    .name(c.getName())
                    .type(c.getType())
                    .build();
            });

        // when
        RecommendationResult rr = itemSelectorEngine.createRecommendation(clothesList, context, 0L, Map.of());
        List<OotdDto> result = rr.items();

        // then
        assertThat(rr.usedRandomFallback()).isTrue();
        assertThat(result).isNotEmpty();
    }

    @Test
    void 후보군이_1개일때_정상적으로_추천된다() {

        // given
        Clothes clothes = Clothes.builder()
            .name("단일 옷")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();
        ReflectionTestUtils.setField(clothes, "id", UUID.randomUUID());

        RecommendationContextDto context = RecommendationContextDto.builder()
            .adjustedTemperature(20.0)
            .currentMonth(Month.APRIL)
            .isRainingOrSnowing(false)
            .build();

        doReturn(0.8).when(itemSelectorEngine).calculateScore(clothes, 20.0, Month.APRIL, false);
        doReturn(OotdDto.builder().name("단일 옷").build()).when(clothesMapper).toOotdDto(any(Clothes.class));

        // when
        RecommendationResult rr = itemSelectorEngine.createRecommendation(List.of(clothes), context, 0L, Map.of());
        List<OotdDto> result = rr.items();

        // then
        assertThat(result).extracting("name").contains("단일 옷");
    }

    @Test
    void 동일_점수_후보_여러개면_균등_샘플링된다() {

        // given
        List<Double> scores = List.of(0.5, 0.5, 0.5, 0.5);
        long seed = 123L;

        // when
        int sample = itemSelectorEngine.softmaxSample(scores, seed);

        // then
        assertThat(sample).isBetween(0, 3);
    }

    @Test
    void 속성명_오타_입력시_기본값으로_처리된다() {

        // given
        ClothesAttribute wrongAttr = ClothesAttribute.builder()
            .definition(ClothesAttributeDef.createClothesAttributeDef("두께오타", List.of("얇음", "보통", "두꺼움")))
            .value("얇음")
            .build();
        Clothes clothes = Clothes.builder()
            .name("오타 옷")
            .type(ClothesType.TOP)
            .attributes(List.of(wrongAttr))
            .build();

        // when
        double score = itemSelectorEngine.calculateScore(clothes, 30, Month.JUNE, false);

        // then
        assertThat(score).isGreaterThan(0.0);
    }

    @Test
    void 옷장이_비어있으면_빈_옷추천_리스트를_반환한다() {

        // given
        RecommendationContextDto context = RecommendationContextDto.builder()
            .adjustedTemperature(20.0)
            .currentMonth(Month.APRIL)
            .isRainingOrSnowing(false)
            .build();

        // when
        RecommendationResult rr = itemSelectorEngine.createRecommendation(List.of(), context, 0L, Map.of());

        // then
        assertThat(rr.items()).isEmpty();
    }

    @Test
    void 속성값_누락시_기본값으로_점수_계산된다() {

        // given
        Clothes clothes = Clothes.builder()
            .name("속성 없는 옷")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();

        // when
        double score = itemSelectorEngine.calculateScore(clothes, 20, Month.APRIL, false);

        // then
        assertThat(score).isGreaterThan(0.0);
    }

    @Test
    void 쿨다운_아이템은_후보에서_제외된다() {
        // given
        UUID cooldownTopId = UUID.randomUUID();

        Clothes top1 = Clothes.builder().name("쿨다운 상의")
            .type(ClothesType.TOP).attributes(List.of()).build();
        ReflectionTestUtils.setField(top1, "id", cooldownTopId);

        Clothes top2 = Clothes.builder().name("정상 상의")
            .type(ClothesType.TOP).attributes(List.of()).build();
        ReflectionTestUtils.setField(top2, "id", UUID.randomUUID());

        RecommendationContextDto ctx = RecommendationContextDto.builder()
            .adjustedTemperature(20.0).currentMonth(Month.APRIL).isRainingOrSnowing(false).build();

        doReturn(0.8).when(itemSelectorEngine).calculateScore(top2, 20.0, Month.APRIL, false);

        Mockito.lenient().when(clothesMapper.toOotdDto(any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().clothesId(c.getId()).name(c.getName()).type(c.getType()).build();
            });

        // when
        RecommendationResult rr = itemSelectorEngine.createRecommendation(
            List.of(top1, top2), ctx, 0L, Map.of(ClothesType.TOP, cooldownTopId)
        );
        List<OotdDto> out = rr.items();

        // then
        assertThat(out).extracting(OotdDto::name).containsExactly("정상 상의");
    }

    @Test
    void 모든_아이템_임계값_미만이면_랜덤폴백_발생() {

        // given
        RecommendationContextDto context = RecommendationContextDto.builder()
            .adjustedTemperature(20.0)
            .currentMonth(Month.MAY)
            .isRainingOrSnowing(false)
            .build();

        Clothes lowTop = Clothes.builder().name("lowTop")
            .type(ClothesType.TOP).build();
        Clothes lowBottom = Clothes.builder().name("lowBottom")
            .type(ClothesType.BOTTOM).build();
        ReflectionTestUtils.setField(lowTop, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(lowBottom, "id", UUID.randomUUID());

        List<Clothes> clothesList = List.of(lowTop, lowBottom);

        Mockito.doReturn(0.1).when(itemSelectorEngine)
            .calculateScore(Mockito.eq(lowTop), Mockito.anyDouble(), Mockito.any(), Mockito.anyBoolean());
        Mockito.doReturn(0.05).when(itemSelectorEngine)
            .calculateScore(Mockito.eq(lowBottom), Mockito.anyDouble(), Mockito.any(), Mockito.anyBoolean());

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().name(c.getName()).type(c.getType()).clothesId(c.getId()).build();
            });

        // when
        RecommendationResult rr = itemSelectorEngine.createRecommendation(clothesList, context, 0L, Map.of());
        List<OotdDto> result = rr.items();

        // then
        assertThat(rr.usedRandomFallback()).isTrue();
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void 코어_아이템_부재시_코어랜덤폴백_추가됨() {

        // given
        RecommendationContextDto context = RecommendationContextDto.builder()
            .adjustedTemperature(20.0)
            .currentMonth(Month.MAY)
            .isRainingOrSnowing(false)
            .build();

        Clothes hat = Clothes.builder().name("hat")
            .type(ClothesType.HAT).build();
        Clothes top = Clothes.builder().name("top")
            .type(ClothesType.TOP).build();
        ReflectionTestUtils.setField(hat, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(top, "id", UUID.randomUUID());

        List<Clothes> clothesList = List.of(hat, top);

        Mockito.doReturn(0.90).when(itemSelectorEngine)
            .calculateScore(Mockito.same(hat), Mockito.eq(20.0), Mockito.eq(Month.MAY), Mockito.eq(false));
        Mockito.doReturn(0.10).when(itemSelectorEngine)
            .calculateScore(Mockito.same(top), Mockito.eq(20.0), Mockito.eq(Month.MAY), Mockito.eq(false));

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().name(c.getName()).type(c.getType()).clothesId(c.getId()).build();
            });

        // when
        RecommendationResult rr = itemSelectorEngine.createRecommendation(clothesList, context, 42L, Map.of());
        List<OotdDto> result = rr.items();

        // then
        assertThat(rr.usedRandomFallback()).isTrue();
        assertThat(result).isNotEmpty();
        assertThat(result.stream().map(OotdDto::type)).contains(ClothesType.TOP);
    }

    @Test
    void 나머지_타입_전부_임계값_미만이면_fallbackRecommendOthers_에서_각타입_하나씩_선택된다() {

        // given
        RecommendationContextDto context = RecommendationContextDto.builder()
            .adjustedTemperature(18.0)
            .currentMonth(Month.MARCH)
            .isRainingOrSnowing(false)
            .build();

        Clothes hat = Clothes.builder().name("hat")
            .type(ClothesType.HAT).build();
        Clothes bag = Clothes.builder().name("bag")
            .type(ClothesType.BAG).build();
        ReflectionTestUtils.setField(hat, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(bag, "id", UUID.randomUUID());
        List<Clothes> clothesList = List.of(hat, bag);

        Mockito.doReturn(0.1).when(itemSelectorEngine)
            .calculateScore(Mockito.any(Clothes.class), Mockito.anyDouble(), Mockito.any(), Mockito.anyBoolean());

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().name(c.getName()).type(c.getType()).clothesId(c.getId()).build();
            });

        // when
        RecommendationResult rr = itemSelectorEngine.createRecommendation(clothesList, context, 99L, Map.of());
        List<OotdDto> result = rr.items();

        // then
        assertThat(rr.usedRandomFallback()).isTrue();
        assertThat(result.stream().map(OotdDto::type)).contains(ClothesType.HAT, ClothesType.BAG);
    }

    @Test
    void recommendTopBottomOrDress_TOP_BOTTOM_둘다_임계값이상이면_두벌_선택된다() {
        // given
        RecommendationContextDto ctx = RecommendationContextDto.builder()
            .adjustedTemperature(18.0)
            .currentMonth(Month.OCTOBER)
            .isRainingOrSnowing(false)
            .build();

        Clothes top = Clothes.builder().name("top").type(ClothesType.TOP).attributes(List.of()).build();
        Clothes bottom = Clothes.builder().name("bottom").type(ClothesType.BOTTOM).attributes(List.of()).build();
        ReflectionTestUtils.setField(top, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(bottom, "id", UUID.randomUUID());

        // 임계값(0.4) 초과 점수로 스텁
        Mockito.doReturn(0.8).when(itemSelectorEngine)
            .calculateScore(Mockito.same(top), Mockito.eq(18.0), Mockito.eq(Month.OCTOBER), Mockito.eq(false));
        Mockito.doReturn(0.8).when(itemSelectorEngine)
            .calculateScore(Mockito.same(bottom), Mockito.eq(18.0), Mockito.eq(Month.OCTOBER), Mockito.eq(false));

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().clothesId(c.getId()).name(c.getName()).type(c.getType()).build();
            });

        Map<ClothesType, List<Clothes>> groups = Map.of(
            ClothesType.TOP, List.of(top),
            ClothesType.BOTTOM, List.of(bottom)
        );
        Map<UUID, Double> cache = new HashMap<>();

        // when
        @SuppressWarnings("unchecked")
        List<OotdDto> out = (List<OotdDto>) ReflectionTestUtils.invokeMethod(
            itemSelectorEngine, "recommendTopBottomOrDress", groups, ctx, 1L, cache);

        // then
        assertThat(out).hasSize(2);
        assertThat(out.stream().map(OotdDto::type)).containsExactlyInAnyOrder(ClothesType.TOP, ClothesType.BOTTOM);
    }

    @Test
    void recommendTopBottomOrDress_DRESS만_임계값이상이면_드레스_단일_선택된다() {
        // given
        RecommendationContextDto ctx = RecommendationContextDto.builder()
            .adjustedTemperature(23.0)
            .currentMonth(Month.JUNE)
            .isRainingOrSnowing(false)
            .build();

        Clothes topLow = Clothes.builder().name("lowTop").type(ClothesType.TOP).attributes(List.of()).build();
        Clothes bottomLow = Clothes.builder().name("lowBottom").type(ClothesType.BOTTOM).attributes(List.of()).build();
        Clothes dressOk = Clothes.builder().name("dressOk").type(ClothesType.DRESS).attributes(List.of()).build();
        ReflectionTestUtils.setField(topLow, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(bottomLow, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(dressOk, "id", UUID.randomUUID());

        // TOP/BOTTOM은 임계값 미만, DRESS는 임계값 초과
        Mockito.doReturn(0.2).when(itemSelectorEngine)
            .calculateScore(Mockito.same(topLow), Mockito.eq(23.0), Mockito.eq(Month.JUNE), Mockito.eq(false));
        Mockito.doReturn(0.2).when(itemSelectorEngine)
            .calculateScore(Mockito.same(bottomLow), Mockito.eq(23.0), Mockito.eq(Month.JUNE), Mockito.eq(false));
        Mockito.doReturn(0.7).when(itemSelectorEngine)
            .calculateScore(Mockito.same(dressOk), Mockito.eq(23.0), Mockito.eq(Month.JUNE), Mockito.eq(false));

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().clothesId(c.getId()).name(c.getName()).type(c.getType()).build();
            });

        Map<ClothesType, List<Clothes>> groups = Map.of(
            ClothesType.TOP, List.of(topLow),
            ClothesType.BOTTOM, List.of(bottomLow),
            ClothesType.DRESS, List.of(dressOk)
        );
        Map<UUID, Double> cache = new HashMap<>();

        // when
        @SuppressWarnings("unchecked")
        List<OotdDto> out = (List<OotdDto>) ReflectionTestUtils.invokeMethod(
            itemSelectorEngine, "recommendTopBottomOrDress", groups, ctx, 7L, cache);

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).type()).isEqualTo(ClothesType.DRESS);
        assertThat(out.get(0).name()).isEqualTo("dressOk");
    }

    @Test
    void recommendTopBottomOrDress_BOTTOM만_임계값이상이면_바텀_단일_선택된다() {
        // given
        RecommendationContextDto ctx = RecommendationContextDto.builder()
            .adjustedTemperature(19.0)
            .currentMonth(Month.MAY)
            .isRainingOrSnowing(false)
            .build();

        Clothes topLow = Clothes.builder().name("lowTop").type(ClothesType.TOP).attributes(List.of()).build();
        Clothes bottomOk = Clothes.builder().name("okBottom").type(ClothesType.BOTTOM).attributes(List.of()).build();
        ReflectionTestUtils.setField(topLow, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(bottomOk, "id", UUID.randomUUID());

        Mockito.doReturn(0.2).when(itemSelectorEngine)
            .calculateScore(Mockito.same(topLow), Mockito.eq(19.0), Mockito.eq(Month.MAY), Mockito.eq(false));
        Mockito.doReturn(0.8).when(itemSelectorEngine)
            .calculateScore(Mockito.same(bottomOk), Mockito.eq(19.0), Mockito.eq(Month.MAY), Mockito.eq(false));

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().clothesId(c.getId()).name(c.getName()).type(c.getType()).build();
            });

        Map<ClothesType, List<Clothes>> groups = Map.of(
            ClothesType.TOP, List.of(topLow),
            ClothesType.BOTTOM, List.of(bottomOk)
        );
        Map<UUID, Double> cache = new HashMap<>();

        // when
        @SuppressWarnings("unchecked")
        List<OotdDto> out = (List<OotdDto>) ReflectionTestUtils.invokeMethod(
            itemSelectorEngine, "recommendTopBottomOrDress", groups, ctx, 5L, cache);

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).type()).isEqualTo(ClothesType.BOTTOM);
        assertThat(out.get(0).name()).isEqualTo("okBottom");
    }

    @Test
    void withHarmonyBonus_후보_style_null이면_점수_그대로() {
        // given
        RecommendationContextDto ctx = RecommendationContextDto.builder()
            .adjustedTemperature(20.0)
            .currentMonth(Month.APRIL)
            .isRainingOrSnowing(false)
            .build();

        // 스타일 속성 없는 아이템 2개
        Clothes a = Clothes.builder().name("A").type(ClothesType.TOP).attributes(List.of()).build();
        Clothes b = Clothes.builder().name("B").type(ClothesType.TOP).attributes(List.of()).build();
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(b, "id", UUID.randomUUID());

        // 기본 점수 스텁
        Mockito.doReturn(0.55).when(itemSelectorEngine)
            .calculateScore(Mockito.same(a), Mockito.eq(20.0), Mockito.eq(Month.APRIL), Mockito.eq(false));
        Mockito.doReturn(0.45).when(itemSelectorEngine)
            .calculateScore(Mockito.same(b), Mockito.eq(20.0), Mockito.eq(Month.APRIL), Mockito.eq(false));

        Map<UUID, Double> cache = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object> scored = (List<Object>) ReflectionTestUtils.invokeMethod(
            itemSelectorEngine, "buildScored", List.of(a, b), ctx, cache);

        // when
        @SuppressWarnings("unchecked")
        List<Object> out = (List<Object>) ReflectionTestUtils.invokeMethod(
            itemSelectorEngine, "withHarmonyBonus", scored, Style.CASUAL, 0.2);

        // then
        Double s0 = (Double) ReflectionTestUtils.getField(out.get(0), "score");
        Double s1 = (Double) ReflectionTestUtils.getField(out.get(1), "score");
        assertThat(s0).isEqualTo(0.55);
        assertThat(s1).isEqualTo(0.45);
    }

    @Test
    void 폴백_hasPairAndHasDress_드레스를_선택한다() {
        // given
        Clothes t1 = Clothes.builder().name("T1").type(ClothesType.TOP).attributes(List.of()).build();
        Clothes b1 = Clothes.builder().name("B1").type(ClothesType.BOTTOM).attributes(List.of()).build();
        Clothes d1 = Clothes.builder().name("D1").type(ClothesType.DRESS).attributes(List.of()).build();
        ReflectionTestUtils.setField(t1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(b1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(d1, "id", UUID.randomUUID());

        Map<ClothesType, List<Clothes>> groups = Map.of(
            ClothesType.TOP, List.of(t1),
            ClothesType.BOTTOM, List.of(b1),
            ClothesType.DRESS, List.of(d1)
        );

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().clothesId(c.getId()).name(c.getName()).type(c.getType()).build();
            });

        // pickDress == true 가 되는 rollCounter 탐색
        long rollCounterForDress = -1L;
        for (long rc = 0; rc < 10_000; rc++) {
            long seed = (long) ReflectionTestUtils.invokeMethod(itemSelectorEngine, "mixSeed", rc, "coreChoice");
            if (new java.util.SplittableRandom(seed).nextInt(2) == 0) {
                rollCounterForDress = rc;
                break;
            }
        }
        assertThat(rollCounterForDress).isNotEqualTo(-1L);

        // when
        @SuppressWarnings("unchecked")
        List<OotdDto> out = (List<OotdDto>) ReflectionTestUtils.invokeMethod(
            itemSelectorEngine, "fallbackRecommendTopBottomOrDress", groups, rollCounterForDress);

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).type()).isEqualTo(ClothesType.DRESS);
        assertThat(out.get(0).name()).isEqualTo("D1");
    }

    @Test
    void 폴백_hasPairAndHasDress_TOP_BOTTOM_한벌을_선택한다() {
        // given
        Clothes t1 = Clothes.builder().name("T1").type(ClothesType.TOP).attributes(List.of()).build();
        Clothes b1 = Clothes.builder().name("B1").type(ClothesType.BOTTOM).attributes(List.of()).build();
        Clothes d1 = Clothes.builder().name("D1").type(ClothesType.DRESS).attributes(List.of()).build();
        ReflectionTestUtils.setField(t1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(b1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(d1, "id", UUID.randomUUID());

        Map<ClothesType, List<Clothes>> groups = Map.of(
            ClothesType.TOP, List.of(t1),
            ClothesType.BOTTOM, List.of(b1),
            ClothesType.DRESS, List.of(d1)
        );

        Mockito.lenient().when(clothesMapper.toOotdDto(Mockito.any(Clothes.class)))
            .thenAnswer(inv -> {
                Clothes c = inv.getArgument(0);
                return OotdDto.builder().clothesId(c.getId()).name(c.getName()).type(c.getType()).build();
            });

        // pickDress == false 가 되는 rollCounter 찾기
        long rollCounterForTopBottom = -1L;
        for (long rc = 0; rc < 10_000; rc++) {
            long seed = (long) ReflectionTestUtils.invokeMethod(itemSelectorEngine, "mixSeed", rc, "coreChoice");
            if (new java.util.SplittableRandom(seed).nextInt(2) != 0) {
                rollCounterForTopBottom = rc;
                break;
            }
        }
        assertThat(rollCounterForTopBottom).isNotEqualTo(-1L);

        // when
        @SuppressWarnings("unchecked")
        List<OotdDto> out = (List<OotdDto>) ReflectionTestUtils.invokeMethod(
            itemSelectorEngine, "fallbackRecommendTopBottomOrDress", groups, rollCounterForTopBottom);

        // then
        assertThat(out).hasSize(2);
        assertThat(out.get(0).type()).isEqualTo(ClothesType.TOP);
        assertThat(out.get(1).type()).isEqualTo(ClothesType.BOTTOM);
        assertThat(out.stream().map(OotdDto::name)).contains("T1", "B1");
    }
}
