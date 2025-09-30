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
import com.samsamotot.otboo.recommendation.type.Style;
import java.time.Month;
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
        List<OotdDto> result = itemSelectorEngine.createRecommendation(
            clothesList,
            context,
            0L,
            Map.of()
        );

        // then
        assertThat(result)
            .extracting("name")
            .contains("추천될 옷")
            .doesNotContain("제외될 옷");
    }

    @Test
    void 카테고리_내_모든_아이템이_임계값_미만이면_추천하지_않는다() {

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

        // when
        List<OotdDto> result = itemSelectorEngine.createRecommendation(
            clothesList,
            context,
            0L,
            Map.of()
        );

        // then
        assertThat(result).isEmpty();
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
        List<OotdDto> result = itemSelectorEngine.createRecommendation(List.of(clothes), context, 0L, Map.of());

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
        List<OotdDto> result = itemSelectorEngine.createRecommendation(
            List.of(),
            context,
            0L,
            Map.of()
        );

        // then
        assertThat(result).isEmpty();
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
}
