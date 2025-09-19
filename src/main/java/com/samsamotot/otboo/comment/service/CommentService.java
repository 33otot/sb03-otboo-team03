package com.samsamotot.otboo.comment.service;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentCursorRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.common.dto.CursorResponse;
import jakarta.validation.Valid;

import java.util.UUID;

public interface CommentService {

    CommentDto create(UUID feedId, CommentCreateRequest request);

    CursorResponse<CommentDto> getComments(UUID feedId, @Valid CommentCursorRequest request);
}
