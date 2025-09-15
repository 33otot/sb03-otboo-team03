package com.samsamotot.otboo.feed.dto;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record FeedDto(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    AuthorDto author,
    WeatherDto weather,
    List<OotdDto> ootds,
    String content,
    long likeCount,
    long commentCount,
    boolean likedByMe
) {

}