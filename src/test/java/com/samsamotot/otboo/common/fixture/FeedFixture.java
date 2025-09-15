package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedClothes;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.test.util.ReflectionTestUtils;

public class FeedFixture {

    public static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String DEFAULT_USER_NAME = "defaultUser";
    public static final String DEFAULT_USER_IMAGE_URL = "http://example.com/profile.jpg";
    private static final String DEFAULT_CONTENT = "기본 피드 내용입니다.";
    private static final Instant DEFAULT_CREATED_AT = Instant.now();

    public static Feed createDefaultFeed() {

        User defaultUser = User.builder().username(DEFAULT_USER_NAME).build();
        ReflectionTestUtils.setField(defaultUser, "id", UUID.randomUUID());

        Weather defaultWeather = Weather.builder().build();
        ReflectionTestUtils.setField(defaultWeather, "id", UUID.randomUUID());

        Clothes defaultClothes = Clothes.builder().build();
        ReflectionTestUtils.setField(defaultClothes, "id", UUID.randomUUID());

        Feed feed = Feed.builder()
            .author(defaultUser)
            .weather(defaultWeather)
            .content(DEFAULT_CONTENT)
            .build();

        FeedClothes feedClothes = FeedClothes.builder()
            .feed(feed)
            .clothes(defaultClothes)
            .build();
        feed.getFeedClothes().add(feedClothes);

        ReflectionTestUtils.setField(feed, "id", UUID.randomUUID());
        return feed;
    }

    public static Feed createFeed(User author, Weather weather, List<FeedClothes> feedClothes) {
        return Feed.builder()
            .author(author)
            .weather(weather)
            .feedClothes(feedClothes)
            .content(DEFAULT_CONTENT)
            .build();
    }

    public static Feed createFeed(User author, Weather weather, List<FeedClothes> feedClothes, String content) {
        return Feed.builder()
            .author(author)
            .weather(weather)
            .feedClothes(feedClothes)
            .content(content)
            .build();
    }

    public static Feed createFeedWithKeyword(String keyword) {
        Feed feed = FeedFixture.createDefaultFeed();
        feed.toBuilder().content(keyword).build();
        return feed;
    }

    public static List<Feed> createFeedsWithSequentialCreationDate(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                Feed feed = createDefaultFeed();
                Instant newCreatedAt = DEFAULT_CREATED_AT.minus(i, ChronoUnit.DAYS);
                ReflectionTestUtils.setField(feed, "createdAt", newCreatedAt);
                return feed;
            })
            .toList();
    }

    public static FeedDto createDefaultFeedDto() {

        return FeedDto.builder()
            .id(UUID.randomUUID())
            .author(createDefaultAuthorDto())
            .content(DEFAULT_CONTENT)
            .likeCount(0L)
            .commentCount(0L)
            .ootds(List.of())
            .weather(null)
            .likedByMe(false)
            .build();
    }

    public static FeedDto createFeedDto(Feed feed) {
        AuthorDto authorDto = AuthorDto.builder()
            .userId(feed.getAuthor().getId())
            .name(feed.getAuthor().getUsername())
            .build();

        return FeedDto.builder()
            .id(feed.getId())
            .author(authorDto)
            .content(feed.getContent())
            .likeCount(feed.getLikeCount())
            .commentCount(feed.getCommentCount())
            .ootds(List.of())
            .weather(null)
            .likedByMe(false)
            .createdAt(feed.getCreatedAt())
            .updatedAt(feed.getUpdatedAt())
            .build();
    }

    public static FeedDto createFeedDto(Feed feed, AuthorDto authorDto, WeatherDto weatherDto, List<OotdDto> ootdDtos) {
        return FeedDto.builder()
            .id(feed.getId())
            .author(authorDto)
            .weather(weatherDto)
            .ootds(ootdDtos)
            .content(feed.getContent())
            .build();
    }

    public static FeedDto createFeedDto(String content, AuthorDto authorDto, WeatherDto weatherDto, List<OotdDto> ootdDtos) {
        return FeedDto.builder()
            .id(UUID.randomUUID())
            .author(authorDto)
            .weather(weatherDto)
            .ootds(ootdDtos)
            .content(content)
            .build();
    }

    public static FeedDto createFeedDtoWithCreatedAt(Instant createdAt) {
        return FeedDto.builder()
            .id(UUID.randomUUID())
            .author(createDefaultAuthorDto())
            .weather(null)
            .ootds(List.of())
            .content(DEFAULT_CONTENT)
            .createdAt(createdAt)
            .build();
    }

    public static AuthorDto createDefaultAuthorDto() {
        return AuthorDto.builder()
            .userId(DEFAULT_USER_ID)
            .name(DEFAULT_USER_NAME)
            .profileImageUrl(DEFAULT_USER_IMAGE_URL)
            .build();
    }
}
