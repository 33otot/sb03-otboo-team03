package com.samsamotot.otboo.comment.repository;

import com.samsamotot.otboo.comment.entity.Comment;
import java.util.List;
import java.util.UUID;

public interface CommentRepositoryCustom {

    List<Comment> findByFeedIdWithCursor(UUID feedId, String cursor, UUID idAfter, int limit);
}
