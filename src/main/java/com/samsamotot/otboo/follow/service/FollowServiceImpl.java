package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.user.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.service
 * FileName     : FollowServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Service
public class FollowServiceImpl implements FollowService {
    // 팔로우 기능
    @Override
    public FollowDto follow(FollowCreateRequest request) {
        // request 값에 있는지 확인
        // 유저 존재 여부 확인 -> 유저 조회 *2
        // 팔로우 되어있는지 확인 -> exception + DB에서도 막아놓음
        // 문제 없으면 생성
        // 유저를 dto로 변환
        // 응답 dto 생성

        User user = User.createUser("test@test.com", "name", "randomPS", new BCryptPasswordEncoder());
        return new FollowDto(UUID.randomUUID(), user, user);
    }
}
