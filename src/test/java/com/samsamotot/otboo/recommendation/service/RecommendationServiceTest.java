package com.samsamotot.otboo.recommendation.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.ProfileFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.recommendation.dto.RecommendationContextDto;
import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recommendation 서비스 단위 테스트")
public class RecommendationServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private ItemSelectorEngine itemSelectorEngine;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    User mockUser;
    Profile mockProfile;
    Weather mockWeather;

    @BeforeEach
    void setUp() {

        // redisTemplate.opsForValue()가 호출되면 항상 가짜 valueOperations를 반환
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        mockUser = UserFixture.createUser();
        mockProfile = ProfileFixture.createProfile(mockUser);
        Grid grid = GridFixture.createGrid();
        mockWeather = WeatherFixture.createWeather(grid);
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(mockWeather, "id", UUID.randomUUID());
    }

    @Test
    void 온도_민감도가_낮은_사용자는_체감온도를_더_낮게_보정한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();
        double sensitivity = 1.0;
        Profile profile = ProfileFixture.createProfile(mockUser, sensitivity);

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));

        ArgumentCaptor<RecommendationContextDto> captor = ArgumentCaptor.forClass(RecommendationContextDto.class);

        // 보정 전 기본 체감 온도
        double baseApparentTemp = mockWeather.getTemperatureCurrent();

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        verify(itemSelectorEngine).createRecommendation(any(), captor.capture(), anyLong(), any());

        RecommendationContextDto capturedContext = captor.getValue();
        assertThat(capturedContext.adjustedTemperature()).isLessThan(baseApparentTemp);
    }

    @Test
    void 온도_민감도가_높은_사용자는_체감온도를_더_높게_보정한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();
        double sensitivity = 5.0;
        Profile profile = ProfileFixture.createProfile(mockUser, sensitivity);

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));

        ArgumentCaptor<RecommendationContextDto> captor = ArgumentCaptor.forClass(RecommendationContextDto.class);

        // 보정 전 기본 체감 온도
        double baseApparentTemp = mockWeather.getTemperatureCurrent();

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        verify(itemSelectorEngine).createRecommendation(any(), captor.capture(), anyLong(), any());

        RecommendationContextDto capturedContext = captor.getValue();
        assertThat(capturedContext.adjustedTemperature()).isGreaterThan(baseApparentTemp);
    }

    @Test
    void Redis에서_roll_카운터와_cooldown_ID를_정상적으로_조회한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();
        String rollCountKey = "reco:" + userId + ":roll";
        String cooldownKey = "reco:" + userId + ":" + ClothesType.TOP + ":cooldown";

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
        given(valueOperations.get(rollCountKey)).willReturn("5");
        given(valueOperations.get(cooldownKey)).willReturn("101");

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        verify(valueOperations, times(1)).get(rollCountKey);
        verify(valueOperations, times(1)).get(cooldownKey);
    }

    @Test
    void Redis에서_조회한_상태값을_ItemSelectorEngine에_올바르게_전달한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();
        String rollCountKey = "reco:" + userId + ":roll";
        String cooldownKey = "reco:" + userId + ":" + ClothesType.TOP + ":cooldown";

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
        given(valueOperations.get(rollCountKey)).willReturn("5");
        given(valueOperations.get(cooldownKey)).willReturn("101");

        ArgumentCaptor<Long> rollCountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<ClothesType, Long>> cooldownCaptor = ArgumentCaptor.forClass(Map.class);

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        // 엔진 호출 시 파라미터를 캡처
        verify(itemSelectorEngine).createRecommendation(any(), any(), rollCountCaptor.capture(), cooldownCaptor.capture());
        // 캡처된 파라미터가 Redis에서 조회한 값과 일치하는지 검증
        assertThat(rollCountCaptor.getValue()).isEqualTo(5L);
        assertThat(cooldownCaptor.getValue()).containsEntry(ClothesType.TOP, 101L);
    }

    @Test
    void Redis에_상태값이_없을경우_기본값으로_엔진을_호출한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
        given(valueOperations.get(any())).willReturn(null); // Redis에서 null 반환

        ArgumentCaptor<Long> rollCountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<ClothesType, Long>> cooldownCaptor = ArgumentCaptor.forClass(Map.class);

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        verify(itemSelectorEngine).createRecommendation(any(), any(), rollCountCaptor.capture(), cooldownCaptor.capture());

        // rollCount는 0L, cooldownIds는 비어있는 상태로 호출되어야 함
        assertThat(rollCountCaptor.getValue()).isEqualTo(0L);
        assertThat(cooldownCaptor.getValue()).isEmpty();
    }

    @Test
    void ItemSelectorEngine이_반환한_결과를_받아_새로운_상태를_Redis에_저장한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();
        String rollCountKey = "reco:" + userId + ":roll";
        String cooldownKey = "reco:" + userId + ":" + ClothesType.TOP + ":cooldown";
        Long newRecommendedTopId = 102L;

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));

        OotdDto topDto = OotdDto.builder()
                .clothesId(UUID.randomUUID())
                .type(ClothesType.TOP)
                .name("테스트 상의")
                .build();

        List<OotdDto> clothes = List.of(topDto);

        RecommendationDto dummyResult = RecommendationDto.builder()
            .userId(userId)
            .weatherId(weatherId)
            .clothes(clothes)
            .build();

        given(itemSelectorEngine.createRecommendation(any(), any(), any(), any())).willReturn(dummyResult);

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        // rollCounter가 1 증가했는지 검증
        verify(valueOperations, times(1)).increment(rollCountKey);
        // 새로운 cooldown ID가 TTL과 함께 저장되었는지 검증
        verify(valueOperations, times(1)).set(eq(cooldownKey),
            eq(topDto.clothesId().toString()), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    void 엔진이_빈_결과를_반환하면_빈_추천_DTO를_반환한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));

        RecommendationDto expected = RecommendationDto.builder()
            .userId(userId)
            .weatherId(weatherId)
            .clothes(Collections.emptyList())
            .build();

        given(itemSelectorEngine.createRecommendation(any(), any(), any(), any())).willReturn(expected);

        // when
        RecommendationDto result = recommendationService.recommendClothes(userId, weatherId);

        // then
        assertThat(result.clothes()).isEmpty();
    }

    @Test
    void 유효하지_않은_weatherId면_예외가_발생한다() {

        // given
        UUID userId = mockUser.getId();
        UUID invalidWeatherId = UUID.randomUUID();

        given(weatherRepository.findById(invalidWeatherId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> recommendationService.recommendClothes(userId, invalidWeatherId))
            .isInstanceOf(OtbooException.class)
            .extracting(e -> ((OtbooException) e).getErrorCode())
            .isEqualTo(ErrorCode.WEATHER_NOT_FOUND);
    }

    @Test
    void 유효하지_않은_userId면_예외가_발생한다() {

        // given
        UUID invalidUserId = UUID.randomUUID();
        UUID weatherId = mockWeather.getId();

        given(profileRepository.findByUserId(invalidUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> recommendationService.recommendClothes(invalidUserId, weatherId))
            .isInstanceOf(OtbooException.class)
            .extracting(e -> ((OtbooException) e).getErrorCode())
            .isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
    }
}
