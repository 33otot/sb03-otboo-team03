package com.samsamotot.otboo.comment.service;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;

public interface CommentService {

    CommentDto create(CommentCreateRequest request);
}
