package com.samsamotot.otboo.comment.service;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentCursorRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.common.dto.CursorResponse;
import jakarta.validation.Valid;
import java.util.UUID;

public interface CommentService {

    CommentDto create(CommentCreateRequest request);

    CursorResponse<CommentDto> getComments(@Valid CommentCursorRequest request, UUID feedId);
}
