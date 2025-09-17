package com.samsamotot.otboo.follow.mapper;

import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
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
    /*  uses = UserSummaryMapper.class,   */
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface FollowMapper {

    @Mapping(target = "followee", source = "followee")
    @Mapping(target = "follower", source = "follower")
    FollowDto toDto(Follow follow);

    // TODO 진짜 UserSummaryDto 만들지기 까지는 profileImageUrl 무시. 아래 코드는 전부 삭제
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "name", source = "username")
    @Mapping(target = "profileImageUrl", ignore = true)
    AuthorDto toAuthorDto(User user);
}
