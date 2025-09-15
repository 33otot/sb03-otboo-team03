package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedClothes;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.entity.Weather;
import java.util.List;
import java.util.UUID;

public class FeedFixture {

    private static final UUID DEFAULT_FEED_ID = UUID.randomUUID();
    private static final String DEFAULT_CONTENT = "피드 테스트";

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
}
