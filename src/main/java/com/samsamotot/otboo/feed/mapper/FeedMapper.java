package com.samsamotot.otboo.feed.mapper;

import com.samsamotot.otboo.clothes.mapper.ClothesMapper;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.weather.mapper.WeatherMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
    uses = {UserMapper.class, ClothesMapper.class, WeatherMapper.class}
)
public interface FeedMapper {

    @Mapping(source = "author", target = "author",  qualifiedByName = "toAuthorDto")
    @Mapping(source = "weather", target = "weather")
    @Mapping(source = "feedClothes", target = "ootds")
    @Mapping(target = "likedByMe", ignore = true)
    FeedDto toDto(Feed feed);
}
