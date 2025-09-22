package com.samsamotot.otboo.user.mapper;

import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "id", target = "userId")
    @Mapping(source = "username", target = "name")
    @Mapping(target = "profileImageUrl", ignore = true)
    AuthorDto toAuthorDto(User user);

    @Mapping(source = "name", target = "username")
    @Mapping(source = "password", target = "password")
    @Mapping(target = "provider", expression = "java(com.samsamotot.otboo.user.entity.Provider.LOCAL)")
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "isLocked", ignore = true)
    @Mapping(target = "temporaryPasswordExpiresAt", ignore = true)
    User toEntity(UserCreateRequest request);

    @Mapping(source = "username", target = "name")
    @Mapping(source = "isLocked", target = "locked")
    UserDto toDto(User user);
}
