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
    User toEntity(UserCreateRequest request);

    @Mapping(source = "username", target = "name")
    UserDto toDto(User user);
}
