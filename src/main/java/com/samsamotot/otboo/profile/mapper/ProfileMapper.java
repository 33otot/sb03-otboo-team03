package com.samsamotot.otboo.profile.mapper;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "userId", source = "profile.user.id")
    @Mapping(target = "location", source = "location")
    ProfileDto toDto(Profile profile);

    @Mapping(target = "x", source = "grid.x")
    @Mapping(target = "y", source = "grid.y")
    WeatherAPILocation toDto(Location location);
}
