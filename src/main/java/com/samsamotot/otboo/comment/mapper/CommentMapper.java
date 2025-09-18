package com.samsamotot.otboo.comment.mapper;

import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CommentMapper {

    @Mapping(source = "author", target = "author")
    @Mapping(source = "feed.id", target = "feedId")
    CommentDto toDto(Comment comment);
}
