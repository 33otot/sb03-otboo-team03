package com.samsamotot.otboo.follow.mapper;

import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * PackageName  : com.samsamotot.otboo.follow.mapper
 * FileName     : FollowMapper
 * Author       : dounguk
 * Date         : 2025. 9. 13.
 */
@Mapper(
    componentModel = "spring",
    uses = UserMapper.class,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface FollowMapper {

    @Mapping(target = "followee", source = "followee")
    @Mapping(target = "follower", source = "follower")
    FollowDto toDto(Follow follow);
}
