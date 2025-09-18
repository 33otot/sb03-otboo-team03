package com.samsamotot.otboo.comment.service;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
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

    @Override
    public CommentDto create(CommentCreateRequest request) {
        return null;
    }
}
