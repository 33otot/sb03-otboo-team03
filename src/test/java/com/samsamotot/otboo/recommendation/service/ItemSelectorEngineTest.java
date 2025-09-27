package com.samsamotot.otboo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesAttribute;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemSelectorEngine 단위 테스트")
public class ItemSelectorEngineTest {

    @Mock
    private ClothesRepository clothesRepository;

    @InjectMocks
    private ItemSelectorEngine itemSelectorEngine;

    ClothesAttributeDef thickness;
    ClothesAttributeDef season;
    ClothesAttributeDef waterproof;

    @BeforeEach
    void setUp() {
        thickness = ClothesAttributeDef.createClothesAttributeDef("두께", List.of("LIGHT", "MID", "HEAVY"));
        season = ClothesAttributeDef.createClothesAttributeDef("계절", List.of("SPRING", "SUMMER", "FALL", "WINTER"));
        waterproof = ClothesAttributeDef.createClothesAttributeDef("방수", List.of("TRUE", "FALSE"));
    }

    @Test
    void 더운_날_가벼운_옷은_두꺼운_옷보다_높은_점수를_받는다() {

        // given
        int hotTemperature = 30;

        ClothesAttribute lightAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("LIGHT")
            .build();

        ClothesAttribute heavyAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("HEAVY")
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
        double lightScore = itemSelectorEngine.calculateScore(lightClothes, hotTemperature, "SUMMER", false);
        double heavyScore = itemSelectorEngine.calculateScore(heavyClothes, hotTemperature, "SUMMER", false);

        // then
        assertThat(lightScore).isGreaterThan(heavyScore);
    }

    @Test
    void 추운_날_두꺼운_옷은_가벼운_옷보다_높은_점수를_받는다() {

        // given
        int coldTemperature = 0;

        ClothesAttribute lightAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("LIGHT")
            .build();

        ClothesAttribute heavyAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("HEAVY")
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
        double lightScore = itemSelectorEngine.calculateScore(lightClothes, coldTemperature, "WINTER", false);
        double heavyScore = itemSelectorEngine.calculateScore(heavyClothes, coldTemperature, "WINTER", false);

        // then
        assertThat(heavyScore).isGreaterThan(lightScore);
    }

    @Test
    void 온화한_날_MID_두께의_옷이_가장_높은_점수를_받는다() {

        // given
        int mildTemperature = 15;

        ClothesAttribute lightAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("LIGHT")
            .build();

        ClothesAttribute midAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("MID")
            .build();

        ClothesAttribute heavyAttr = ClothesAttribute.builder()
            .definition(thickness)
            .value("HEAVY")
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
        double lightScore = itemSelectorEngine.calculateScore(lightClothes, mildTemperature, "FALL", false);
        double midScore = itemSelectorEngine.calculateScore(midClothes, mildTemperature, "FALL", false);
        double heavyScore = itemSelectorEngine.calculateScore(heavyClothes, mildTemperature, "FALL", false);

        // then
        assertThat(midScore).isGreaterThan(heavyScore);
        assertThat(midScore).isGreaterThan(lightScore);
    }

    @Test
    void 계절_일치_옷은_추가_점수를_받는다() {

        // given
        String currentSeason = "SUMMER";

        ClothesAttribute summerSeasonAttr = ClothesAttribute.builder()
            .definition(season)
            .value("SUMMER")
            .build();

        ClothesAttribute winterSeasonAttr = ClothesAttribute.builder()
            .definition(season)
            .value("WINTER")
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
        double summerScore = itemSelectorEngine.calculateScore(summerClothes, 30, currentSeason, false);
        double winterScore = itemSelectorEngine.calculateScore(winterClothes, 30, currentSeason, false);

        // then
        assertThat(summerScore).isGreaterThan(winterScore);
    }

    @Test
    void 비오는_날_방수_가능_옷은_추가_점수를_받는다() {

        // given
        boolean isRaining = true;

        ClothesAttribute waterproofAttr = ClothesAttribute.builder()
            .definition(waterproof)
            .value("TRUE")
            .build();

        ClothesAttribute nonWaterproofAttr = ClothesAttribute.builder()
            .definition(waterproof)
            .value("FALSE")
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
        double waterproofScore = itemSelectorEngine.calculateScore(waterproofClothes, 20, "SPRING", isRaining);
        double nonWaterproofScore = itemSelectorEngine.calculateScore(nonWaterproofClothes, 20, "SPRING", isRaining);

        // then
        assertThat(waterproofScore).isGreaterThan(nonWaterproofScore);
    }

    @Test
    void 비오는_날_방수_불가능_옷은_추가_점수를_받지_않는다() {

        // given
        boolean isRaining = false;

        ClothesAttribute waterproofAttr = ClothesAttribute.builder()
            .definition(waterproof)
            .value("TRUE")
            .build();

        ClothesAttribute nonWaterproofAttr = ClothesAttribute.builder()
            .definition(waterproof)
            .value("FALSE")
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
        double waterproofScore = itemSelectorEngine.calculateScore(waterproofClothes, 20, "SPRING", isRaining);
        double nonWaterproofScore = itemSelectorEngine.calculateScore(nonWaterproofClothes, 20, "SPRING", isRaining);

        // then
        assertThat(waterproofScore).isEqualTo(nonWaterproofScore);
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
        int MIN_SCORE_THRESHOLD = 10;
        int temperature = 20;
        String season = "SPRING";
        boolean isRainy = false;

        Clothes highScoreClothes = Clothes.builder()
            .name("추천될 옷")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();

        Clothes lowScoreClothes = Clothes.builder()
            .name("제외될 옷")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();

        List<Clothes> clothesList = List.of(highScoreClothes, lowScoreClothes);

        given(clothesRepository.findAllByType(any())).willReturn(clothesList);
        given(itemSelectorEngine.calculateScore(highScoreClothes, temperature, season, isRainy)).willReturn(15.0);
        given(itemSelectorEngine.calculateScore(lowScoreClothes, temperature, season, isRainy)).willReturn(5.0);

        // when
        RecommendationDto result = itemSelectorEngine.createRecommendation(
            null,
            null,
            0L,
            Map.of()
        );

        // then
        assertThat(result.clothes())
            .extracting("name")
            .contains("추천될 옷")
            .doesNotContain("제외될 옷");
    }

    @Test
    void 카테고리_내_모든_아이템이_임계값_미만이면_추천하지_않는다() {

        // given
        int MIN_SCORE_THRESHOLD = 10;
        int temperature = 20;
        String season = "SPRING";
        boolean isRainy = false;

        Clothes lowScoreClothes1 = Clothes.builder()
            .name("제외될 옷1")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();

        Clothes lowScoreClothes2 = Clothes.builder()
            .name("제외될 옷2")
            .type(ClothesType.TOP)
            .attributes(List.of())
            .build();

        List<Clothes> clothesList = List.of(lowScoreClothes1, lowScoreClothes2);

        given(clothesRepository.findAllByType(any())).willReturn(clothesList);
        given(itemSelectorEngine.calculateScore(lowScoreClothes1, temperature, season, isRainy)).willReturn(5.0);
        given(itemSelectorEngine.calculateScore(lowScoreClothes2, temperature, season, isRainy)).willReturn(0.0);

        // when
        RecommendationDto result = itemSelectorEngine.createRecommendation(
            null,
            null,
            0L,
            Map.of()
        );

        // then
        assertThat(result.clothes()).isEmpty();
    }

    @Test
    void 옷장이_비어있으면_빈_옷추천_리스트를_반환한다() {

        // given
        given(clothesRepository.findAllByType(any())).willReturn(List.of());

        // when
        RecommendationDto result = itemSelectorEngine.createRecommendation(
            null,
            null,
            0L,
            Map.of()
        );

        // then
        assertThat(result.clothes()).isEmpty();
    }
}
