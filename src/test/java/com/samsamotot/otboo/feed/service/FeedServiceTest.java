package com.samsamotot.otboo.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedClothes;
import com.samsamotot.otboo.feed.fixture.FeedFixture;
import com.samsamotot.otboo.feed.mapper.FeedMapper;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class FeedServiceTest {

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private FeedMapper feedMapper;

    @InjectMocks
    private FeedServiceImpl feedService;

    @Nested
    @DisplayName("피드 생성 테스트")
    class FeedCreateTests {

        @Test
        void 피드를_생성하면_FeedDto를_반환해야한다() {

            // given
            UUID userId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(clothesId);

            User mockUser = User.builder().build();
            Weather mockWeather = Weather.builder().build();
            Clothes mockClothes = Clothes.builder().build();
            List<Clothes> mockClothesList = List.of(mockClothes);
            List<FeedClothes> mockFeedClothesList = List.of(FeedClothes.builder().build());

            String content = "피드 등록 테스트";

            FeedCreateRequest request = FeedCreateRequest.builder()
                .authorId(userId)
                .weatherId(weatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            Feed savedFeed = FeedFixture.createFeed(mockUser, mockWeather, mockFeedClothesList, content);
            ReflectionTestUtils.setField(savedFeed, "id", UUID.randomUUID());

            FeedDto expectedDto = FeedDto.builder()
                .id(savedFeed.getId())
                .content(savedFeed.getContent())
                .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
            given(clothesRepository.findAllById(clothesIds)).willReturn(mockClothesList);
            given(feedRepository.save(any(Feed.class))).willReturn(savedFeed);
            given(feedMapper.toDto(savedFeed)).willReturn(expectedDto);

            // when
            FeedDto result = feedService.create(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(expectedDto.id());
            assertThat(result.content()).isEqualTo(expectedDto.content());
        }

        @Test
        void 피드를_등록할_때_존재하지_않는_유저면_예외가_발생한다() {

            // given
            UUID invalidUserId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(UUID.randomUUID());

            String content = "피드 등록 테스트";

            FeedCreateRequest request = FeedCreateRequest.builder()
                .authorId(invalidUserId)
                .weatherId(weatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            given(userRepository.findById(invalidUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.create(request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(weatherRepository, never()).findById(any());
            verify(clothesRepository, never()).findAllById(any());
            verify(feedRepository, never()).save(any());
        }

        @Test
        void 피드를_등록할_때_존재하지_않는_날씨면_예외가_발생한다1() {

            // given
            UUID userId = UUID.randomUUID();
            UUID invalidWeatherId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(UUID.randomUUID());

            User mockUser = User.builder().build();

            String content = "피드 등록 테스트";

            FeedCreateRequest request = FeedCreateRequest.builder()
                .authorId(userId)
                .weatherId(invalidWeatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(weatherRepository.findById(invalidWeatherId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.create(request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.WEATHER_NOT_FOUND);

            verify(userRepository).findById(userId);
            verify(clothesRepository, never()).findAllById(any());
            verify(feedRepository, never()).save(any());
        }

        @Test
        void 피드를_등록할_때_존재하지_않는_의상이면_예외가_발생한다1() {

            // given
            UUID userId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            List<UUID> invalidClothesIds = List.of(UUID.randomUUID());

            User mockUser = User.builder().build();
            Weather mockWeather = Weather.builder().build();

            String content = "피드 등록 테스트";

            FeedCreateRequest request = FeedCreateRequest.builder()
                .authorId(userId)
                .weatherId(weatherId)
                .clothesIds(invalidClothesIds)
                .content(content)
                .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(weatherRepository.findById(weatherId)).willReturn(Optional.of(mockWeather));
            given(clothesRepository.findAllById(invalidClothesIds)).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> feedService.create(request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLOTHES_NOT_FOUND);

            verify(feedRepository, never()).save(any());
        }
    }
}
