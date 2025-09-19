package com.samsamotot.otboo.comment.repository;

import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.feed.entity.Feed;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentRepositoryCustom {

    long countByFeed(Feed feed);
}
