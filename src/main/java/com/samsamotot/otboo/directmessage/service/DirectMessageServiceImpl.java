package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.repository.DirectMessageRepository;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.service
 * FileName     : DirectMessageServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Slf4j
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {
    private static final String DM_SERVICE = "[DirectMessageService] ";

    private final DirectMessageRepository directMessageRepository;

    private final UserRepository userRepository;

    private final FollowRepository followRepository;

    @Override
    public CursorResponse<DirectMessage> getMessages(MessageRequest request) {
        UUID loggedInUserId = UUID.randomUUID();

        UUID uuid = request.followerId();
        userRepository.findById(loggedInUserId)
            .orElseThrow(() -> {
                log.warn(DM_SERVICE + "팔로워 사용자 없음: followerId={}", request.followerId());
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
            });


        return null;
    }
}
