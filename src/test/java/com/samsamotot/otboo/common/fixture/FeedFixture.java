package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedClothes;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class FeedFixture {

    private static final String DEFAULT_CONTENT = "기본 피드 내용입니다.";

    public static Feed createFeed(User user, Weather weather) {
        Feed feed = Feed.builder()
            .author(user)
            .weather(weather)
            .content(DEFAULT_CONTENT)
            .build();
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

    public static Feed createFeedWithKeyword(User user, Weather weather, String keyword) {
        Feed feed = FeedFixture.createFeed(user, weather);
        return feed.toBuilder().content(keyword).build();
    }

    public static Feed createFeedWithCreatedAt(User user, Weather weather, Instant createdAt) {
        Feed feed = FeedFixture.createFeed(user, weather);
        ReflectionTestUtils.setField(feed, "createdAt", createdAt);
        return feed;
    }

    public static Feed createFeedWithLikeCount(User user, Weather weather, long likeCount) {
        Feed feed = FeedFixture.createFeed(user, weather);
        return feed.toBuilder().likeCount(likeCount).build();
    }

    public static FeedDto createFeedDto(Feed feed) {
        return FeedDto.builder()
            .id(feed.getId())
            .author(UserFixture.createAuthorDto(feed.getAuthor()))
            .content(feed.getContent())
            .likeCount(feed.getLikeCount())
            .commentCount(feed.getCommentCount())
            .ootds(List.of())
            .weather(WeatherFixture.createWeatherDto(feed.getWeather()))
            .likedByMe(false)
            .createdAt(feed.getCreatedAt())
            .updatedAt(feed.getUpdatedAt())
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

    public static FeedDto createFeedDtoWithCreatedAt(String content, AuthorDto authorDto, WeatherDto weatherDto, List<OotdDto> ootdDtos, Instant createdAt) {
        return FeedDto.builder()
            .id(UUID.randomUUID())
            .author(authorDto)
            .weather(weatherDto)
            .ootds(ootdDtos)
            .content(content)
            .createdAt(createdAt)
            .build();
    }

    public static FeedDto createFeedDtoWithContent(Feed feed, String content) {
        return FeedDto.builder()
            .id(feed.getId())
            .author(UserFixture.createAuthorDto(feed.getAuthor()))
            .content(content)
            .likeCount(feed.getLikeCount())
            .commentCount(feed.getCommentCount())
            .ootds(List.of())
            .weather(WeatherFixture.createWeatherDto(feed.getWeather()))
            .likedByMe(false)
            .createdAt(feed.getCreatedAt())
            .updatedAt(feed.getUpdatedAt())
            .build();
    }
}