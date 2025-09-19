package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.user.entity.User;

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
}
