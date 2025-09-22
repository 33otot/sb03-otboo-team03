package com.samsamotot.otboo.comment.controller;

import com.samsamotot.otboo.comment.controller.api.CommentApi;
import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentCursorRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.service.CommentService;
import com.samsamotot.otboo.common.dto.CursorResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피드 댓글(Comment) 관련 API 요청을 처리하는 REST 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds/{feedId}/comments")
public class CommentController implements CommentApi {

    private static final String CONTROLLER = "[CommentController] ";

    private final CommentService commentService;

    /**
     * 특정 피드에 새로운 댓글을 생성합니다.
     *
     * <p>요청 본문({@link CommentCreateRequest})에 피드 ID, 작성자 ID, 댓글 내용을 포함해야 합니다.
     * 성공 시, 생성된 댓글 정보({@link CommentDto})와 함께 201 Created 상태 코드를 반환합니다.
     *
     * @param commentCreateRequest 댓글 생성 요청 DTO
     * @return 생성된 댓글 정보를 담은 ResponseEntity (HTTP 201 Created)
     */
    @Override
    @PostMapping
    public ResponseEntity<CommentDto> createComment(
        @PathVariable("feedId") UUID feedId,
        @Valid @RequestBody CommentCreateRequest commentCreateRequest
    ) {
        log.info(CONTROLLER + "피드 댓글 등록 요청: feedId = {}", feedId);
        CommentDto result = commentService.create(feedId, commentCreateRequest);
        log.info(CONTROLLER + "피드 댓글 등록 완료: {}", result);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(result);
    }

    /**
     * 특정 피드에 달린 댓글 목록을 커서 기반 페이지네이션으로 조회합니다.
     *
     * @param feedId 댓글을 조회할 피드의 ID
     * @param commentCursorRequest 페이지네이션을 위한 커서 요청 정보 DTO (cursor, idAfter, limit)
     * @return 댓글 목록과 다음 페이지 정보를 담은 CursorResponse를 포함하는 ResponseEntity (HTTP 200 OK)
     */
    @Override
    @GetMapping
    public ResponseEntity<CursorResponse<CommentDto>> getComments(
        @PathVariable("feedId") UUID feedId,
        @Valid @ParameterObject @ModelAttribute CommentCursorRequest commentCursorRequest
    ) {
        log.info(CONTROLLER + "피드 댓글 목록 조회 요청: feedId = {}, request = {}", feedId, commentCursorRequest);
        CursorResponse<CommentDto> result = commentService.getComments(feedId, commentCursorRequest);
        log.info(CONTROLLER + "피드 댓글 목록 조회 완료: {}개", result.totalCount());

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }
}
