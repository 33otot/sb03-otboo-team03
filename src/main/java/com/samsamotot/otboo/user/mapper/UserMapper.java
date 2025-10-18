package com.samsamotot.otboo.user.mapper;

import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.dto.UserDto;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public abstract class UserMapper {

    @Autowired
    ProfileRepository profileRepository;

    @Named("toAuthorDto")
    @Mapping(source = "id", target = "userId")
    @Mapping(source = "username", target = "name")
    @Mapping(source = "id", target = "profileImageUrl", qualifiedByName = "profileUrlByUserId")
    public abstract AuthorDto toAuthorDto(User user);

    @Mapping(source = "name", target = "username")
    @Mapping(source = "password", target = "password")
    @Mapping(target = "provider", expression = "java(com.samsamotot.otboo.user.entity.Provider.LOCAL)")
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "isLocked", ignore = true)
    @Mapping(target = "temporaryPasswordExpiresAt", ignore = true)
    public abstract User toEntity(UserCreateRequest request);

    @Mapping(source = "username", target = "name")
    @Mapping(source = "isLocked", target = "locked")
    public abstract UserDto toDto(User user);

    @Named("profileUrlByUserId")
    String profileUrlByUserId(UUID userId) {
        if (userId == null) return null;
        return profileRepository.findByUserId(userId)
            .map(Profile::getProfileImageUrl)
            .orElse(null);
    }
}
