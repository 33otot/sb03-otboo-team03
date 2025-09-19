package com.samsamotot.otboo.comment.repository;

import com.samsamotot.otboo.comment.entity.Comment;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    @Override
    public List<Comment> findByFeedIdWithCursor(UUID feedId, String cursor, UUID idAfter,
        int limit) {
        return List.of();
    }
}
