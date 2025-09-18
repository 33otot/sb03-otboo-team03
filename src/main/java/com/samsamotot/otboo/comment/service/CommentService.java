package com.samsamotot.otboo.comment.service;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import java.util.UUID;

public interface CommentService {

    CommentDto create(UUID feedId, CommentCreateRequest request);
}
