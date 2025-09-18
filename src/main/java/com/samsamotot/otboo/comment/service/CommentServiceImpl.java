package com.samsamotot.otboo.comment.service;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentCursorRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.comment.mapper.CommentMapper;
import com.samsamotot.otboo.comment.repository.CommentRepository;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글(Comment) 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private static final String SERVICE = "[CommentServiceImpl] ";

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final CommentMapper commentMapper;

    /**
     * 새로운 댓글을 생성합니다.
     *
     * @param feedId  댓글을 생성할 피드 ID
     * @param request 댓글 생성 요청 DTO
     * @return 생성된 댓글의 정보를 담은 DTO
     * @throws OtbooException 사용자를 찾을 수 없거나 피드를 찾을 수 없는 경우
     */
    @Override
    public CommentDto create(UUID feedId, CommentCreateRequest request) {

        UUID authorId = request.authorId();
        String content = request.content();

        log.debug(SERVICE + "댓글 생성 시작: authorId = {}, feedId = {}", authorId, feedId);

        User user = userRepository.findById(authorId)
            .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));
        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> new OtbooException(ErrorCode.FEED_NOT_FOUND));

        Comment comment = Comment.builder()
            .author(user)
            .feed(feed)
            .content(content)
            .build();

        Comment saved = commentRepository.save(comment);
        CommentDto result = commentMapper.toDto(saved);

        log.debug(SERVICE + "댓글 생성 완료: commentId = {}", saved.getId());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<CommentDto> getComments(CommentCursorRequest request, UUID feedId) {
        return null;
    }
}
