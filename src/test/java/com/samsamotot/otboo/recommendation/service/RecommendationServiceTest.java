package com.samsamotot.otboo.recommendation.service;


import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.*;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.recommendation.dto.RecommendationContextDto;
import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recommendation 서비스 단위 테스트")
public class RecommendationServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private ItemSelectorEngine itemSelectorEngine;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    User mockUser;
    Profile mockProfile;
    Weather mockWeather;
    List<Clothes> mockClothesList;

    @BeforeEach
    void setUp() {

        // Redis ValueOperations,HashOperations 모킹 설정 (테스트마다 필요하지 않을 수 있어 lenient 사용)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        mockUser = UserFixture.createUser();
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        mockProfile = ProfileFixture.createProfile(mockUser);
        ReflectionTestUtils.setField(mockProfile, "id", UUID.randomUUID());

        Grid grid = GridFixture.createGrid();
        mockWeather = WeatherFixture.createWeather(grid);
        ReflectionTestUtils.setField(mockWeather, "id", UUID.randomUUID());

        Clothes clothes = ClothesFixture.createClothes();
        ReflectionTestUtils.setField(clothes, "id", UUID.randomUUID());
        mockClothesList = List.of(clothes);
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
        given(clothesRepository.findAllByOwnerId(userId)).willReturn(mockClothesList);

        ArgumentCaptor<RecommendationContextDto> captor = ArgumentCaptor.forClass(RecommendationContextDto.class);

        // 보정 전 기본 체감 온도
        double baseApparentTemp = RecommendationServiceImpl.calculateFeelsLike(
            mockWeather.getTemperatureCurrent(),
            mockWeather.getWindSpeed(),
            mockWeather.getHumidityCurrent()
        );

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
        given(clothesRepository.findAllByOwnerId(userId)).willReturn(mockClothesList);

        ArgumentCaptor<RecommendationContextDto> captor = ArgumentCaptor.forClass(RecommendationContextDto.class);

        // 보정 전 기본 체감 온도
        double baseApparentTemp = RecommendationServiceImpl.calculateFeelsLike(
            mockWeather.getTemperatureCurrent(),
            mockWeather.getWindSpeed(),
            mockWeather.getHumidityCurrent()
        );

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
        String rollCountKey = "rollCounter:" + userId;
        String cooldownKey = "cooldownIdMap:" + userId;
        Object cooldownField = ClothesType.TOP;

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
        given(clothesRepository.findAllByOwnerId(userId)).willReturn(mockClothesList);
        given(valueOperations.get(rollCountKey)).willReturn(5L);
        given(hashOperations.entries(cooldownKey)).willReturn(Map.of(cooldownField, UUID.randomUUID().toString()));

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        verify(valueOperations, times(1)).get(rollCountKey);
        verify(hashOperations, times(1)).entries(cooldownKey);
    }

    @Test
    void Redis에서_조회한_상태값을_ItemSelectorEngine에_올바르게_전달한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();
        UUID randomClothesId = UUID.randomUUID();
        String rollCountKey = "rollCounter:" + userId;
        String cooldownKey = "cooldownIdMap:" + userId;
        Object cooldownField = ClothesType.TOP;

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
        given(clothesRepository.findAllByOwnerId(userId)).willReturn(mockClothesList);
        given(valueOperations.get(rollCountKey)).willReturn(5L);
        given(hashOperations.entries(cooldownKey)).willReturn(Map.of(cooldownField, randomClothesId.toString()));

        ArgumentCaptor<Long> rollCountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map> cooldownCaptor = ArgumentCaptor.forClass(Map.class);

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        // 엔진 호출 시 파라미터를 캡처
        verify(itemSelectorEngine).createRecommendation(any(), any(), rollCountCaptor.capture(), cooldownCaptor.capture());
        // 캡처된 파라미터가 Redis에서 조회한 값과 일치하는지 검증
        assertThat(rollCountCaptor.getValue()).isEqualTo(5L);
        assertThat(cooldownCaptor.getValue()).containsEntry(ClothesType.TOP, randomClothesId);
    }

    @Test
    void Redis에_상태값이_없을경우_기본값으로_엔진을_호출한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
        given(clothesRepository.findAllByOwnerId(userId)).willReturn(mockClothesList);
        given(valueOperations.get(any())).willReturn(null); // Redis에서 null 반환

        ArgumentCaptor<Long> rollCountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map> cooldownCaptor = ArgumentCaptor.forClass(Map.class);

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
        UUID randomClothesId = UUID.randomUUID();
        String rollCountKey = "rollCounter:" + userId;
        String cooldownKey = "cooldownIdMap:" + userId;
        Object cooldownField = ClothesType.TOP;

        OotdDto topDto = OotdDto.builder()
                .clothesId(UUID.randomUUID())
                .type(ClothesType.TOP)
                .name("테스트 상의")
                .build();

        List<OotdDto> clothes = List.of(topDto);

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
        given(clothesRepository.findAllByOwnerId(userId)).willReturn(mockClothesList);
        given(itemSelectorEngine.createRecommendation(any(), any(), anyLong(), any())).willReturn(clothes);

        doAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            callback.doInRedis(null);
            return null;
        }).when(redisTemplate).executePipelined(any(RedisCallback.class));

        // when
        recommendationService.recommendClothes(userId, weatherId);

        // then
        verify(valueOperations, times(1)).set(eq(rollCountKey), eq(1L));
        verify(hashOperations, times(1)).put(eq(cooldownKey), eq(topDto.type().name()), eq(topDto.clothesId().toString()));
    }

    @Test
    void 엔진이_빈_결과를_반환하면_빈_추천_DTO를_반환한다() {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = mockWeather.getId();

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
        given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
        given(clothesRepository.findAllByOwnerId(userId)).willReturn(mockClothesList);

        List<OotdDto> clothes = List.of();

        given(itemSelectorEngine.createRecommendation(any(), any(), anyLong(), any())).willReturn(clothes);

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

        given(profileRepository.findByUserId(userId)).willReturn(Optional.of(mockProfile));
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
