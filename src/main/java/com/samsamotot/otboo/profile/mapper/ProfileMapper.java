package com.samsamotot.otboo.profile.mapper;

import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.entity.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "userId", source = "profile.user.id")
    @Mapping(target = "locationId", source = "profile.location.id")
    ProfileDto toDto(Profile profile);
}
