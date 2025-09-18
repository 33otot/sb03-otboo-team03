package com.samsamotot.otboo.comment.mapper;

import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.entity.Comment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    CommentDto toDto(Comment comment);
}
