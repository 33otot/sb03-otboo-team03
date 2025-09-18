package com.samsamotot.otboo.comment.controller;

import com.samsamotot.otboo.comment.controller.api.CommentApi;
import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/feeds/{feedId}")
public class CommentController implements CommentApi {

    private final String CONTROLLER = "[CommentController] ";

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
    @PostMapping("/comments")
    public ResponseEntity<CommentDto> createComment(
        @Valid @RequestBody CommentCreateRequest commentCreateRequest
    ) {
        log.info(CONTROLLER + "피드 댓글 등록 요청: {}", commentCreateRequest);
        CommentDto result = commentService.create(commentCreateRequest);
        log.info(CONTROLLER + "피드 댓글 등록 완료: {}", result);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(result);
    }
}
