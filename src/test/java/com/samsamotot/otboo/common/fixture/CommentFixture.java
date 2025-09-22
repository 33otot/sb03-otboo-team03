package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import java.time.Instant;
import java.util.UUID;

public class CommentFixture {

    private static final String DEFAULT_CONTENT = "기본 댓글 내용입니다.";

    public static Comment createComment(Feed feed, User author) {
        return Comment.builder()
            .feed(feed)
            .author(author)
            .content(DEFAULT_CONTENT)
            .build();
    }

    public static Comment createCommentWithContent (Feed feed, User author, String content) {
        return Comment.builder()
            .feed(feed)
            .author(author)
            .content(content)
            .build();
    }

    public static CommentDto createCommentDto(Comment comment) {
        return CommentDto.builder()
            .id(comment.getId())
            .author(UserFixture.createAuthorDto(comment.getAuthor()))
            .feedId(comment.getFeed().getId())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .build();
    }

    public static CommentDto createCommentDtoWithCreatedAt(String content, AuthorDto authorDto, UUID feedId, Instant createdAt) {
        return CommentDto.builder()
            .author(authorDto)
            .feedId(feedId)
            .content(content)
            .createdAt(createdAt)
            .build();
    }

    public static CommentDto createCommentDtoWithIdAndCreatedAt(UUID id, String content, AuthorDto authorDto, UUID feedId, Instant createdAt) {
        return CommentDto.builder()
            .id(id)
            .author(authorDto)
            .feedId(feedId)
            .content(content)
            .createdAt(createdAt)
            .build();
    }
}
