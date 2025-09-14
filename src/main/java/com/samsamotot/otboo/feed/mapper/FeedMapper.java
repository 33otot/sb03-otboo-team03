package com.samsamotot.otboo.feed.mapper;

import com.samsamotot.otboo.clothes.mapper.ClothesMapper;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.weather.mapper.WeatherMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class, ClothesMapper.class, WeatherMapper.class}
)
public interface FeedMapper {

    @Mapping(source = "author", target = "author")
    @Mapping(source = "weather", target = "weather")
    @Mapping(source = "feedClothes", target = "ootds")
    @Mapping(target = "likedByMe", ignore = true)
    FeedDto toDto(Feed feed);
}
