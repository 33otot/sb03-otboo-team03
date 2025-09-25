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
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
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
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

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

        feed.incrementCommentCount(); // 댓글 수 증가

        CommentDto result = commentMapper.toDto(saved);

        log.debug(SERVICE + "댓글 생성 완료: commentId = {}", saved.getId());

        return result;
    }

    /**
     * 특정 피드의 댓글 목록을 커서 기반 페이지네이션으로 조회합니다.
     *
     * @param feedId    댓글을 조회할 피드의 ID.
     * @param request   페이지네이션 파라미터({@code limit}, {@code cursor}, {@code idAfter})를 담고 있는 요청 DTO
     * @return 댓글 DTO 리스트와 페이지네이션 정보를 포함하는 {@link CursorResponse<CommentDto>}
     */
    @Override
    @Transactional(readOnly = true)
    public CursorResponse<CommentDto> getComments(UUID feedId, CommentCursorRequest request) {

        log.debug(SERVICE + "댓글 목록 조회 시작: request = {}, feedId = {}", request, feedId);

        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> new OtbooException(ErrorCode.FEED_NOT_FOUND));

        String cursor = request.cursor();
        UUID idAfter = request.idAfter();
        int limit = sanitizeLimit(request.limit());

        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;

        if (cursor != null) {
            validateCursorFormat(cursor);
        }

        List<Comment> comments = commentRepository.findByFeedIdWithCursor(feedId, cursor, idAfter, limit + 1);

        boolean hasNext = comments.size() > limit;
        String nextCursor = null;
        UUID nextIdAfter =null;

        if (hasNext) {
            Comment lastComment = comments.get(limit - 1);
            nextCursor = lastComment.getCreatedAt().toString();
            nextIdAfter = lastComment.getId();
            comments = comments.subList(0, limit);
        }

        long totalCount = commentRepository.countByFeedId(feedId);

        List<CommentDto> commentDtos = comments.stream()
            .map(commentMapper::toDto)
            .toList();

        log.debug(SERVICE + "댓글 목록 조회 완료 - 조회된 댓글 수: {}, hasNext: {}", commentDtos.size(), hasNext);

        return new CursorResponse<>(
            commentDtos,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }

    private Instant validateCursorFormat(String cursorValue) {

        try {
            return Instant.parse(cursorValue);
        } catch (DateTimeParseException e) {
            throw new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT, Map.of("cursor", cursorValue));
        }
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null || limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}
