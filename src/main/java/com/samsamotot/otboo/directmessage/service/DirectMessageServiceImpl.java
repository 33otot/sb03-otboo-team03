package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.directmessage.dto.*;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.mapper.DirectMessageMapper;
import com.samsamotot.otboo.directmessage.repository.DirectMessageRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.service
 * FileName     : DirectMessageServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageServiceImpl implements DirectMessageService {
    private static final String DM_SERVICE = "[DirectMessageService] ";

    private final DirectMessageRepository directMessageRepository;

    private final UserRepository userRepository;

    private final DirectMessageMapper directMessageMapper;

    private final EntityManager em;

    // TODO DM 목록 조회 : me 정보 가져오는 로직 수정 필요
    @Override
    public DirectMessageListResponse getMessages(MessageRequest request) {
        log.info(DM_SERVICE + "DM 목록 조회 시작 - request: {}", request);
        UUID myId = UUID.fromString("a0000000-0000-0000-0000-000000000001"); // TODO 추후 currentUserId() 로 수정
        UUID otherId = request.userId();

        User me = userRepository.findById(myId)
            .orElseThrow(() -> {
                log.warn(DM_SERVICE + " 사용자 없음: userId={}", request.userId());
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
            });

        if (me.isLocked()) {
            log.warn(DM_SERVICE + "잠금 계정: userId={}", me.getId());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

        User other = userRepository.findById(otherId)
            .orElseThrow(() -> {
                log.warn(DM_SERVICE + " 사용자 없음: userId={}", request.userId());
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
            });

        if (other.isLocked()) {
            log.warn(DM_SERVICE + "잠금 계정: userId={}", other.getId());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

        Instant cursor = parseCursor(request.cursor());
        UUID idAfter = request.idAfter();
        int limit = Math.max(1, request.limit());

        List<DirectMessage> rows = directMessageRepository.findBetweenWithCursor(
            myId, otherId, cursor, idAfter, PageRequest.of(0, limit + 1)
        );

        boolean hasNext = rows.size() > limit;
        List<DirectMessage> data = hasNext ? rows.subList(0, limit) : rows;

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !data.isEmpty()) {
            DirectMessage last = data.get(data.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        long total = directMessageRepository.countBetween(myId, otherId);

        log.info(DM_SERVICE + "DM 목록 조회 완료 - total: {}, hasNext: {}", total, hasNext);

        return DirectMessageListResponse.builder()
            .data(data.stream().map(directMessageMapper::toDto).toList())
            .nextCursor(nextCursor)
            .nextIdAfter(nextIdAfter)
            .hasNext(hasNext)
            .totalCount(total)
            .sortBy("createdAt")
            .sortDirection("DESCENDING")
            .build();
    }

    private static Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return Instant.parse(cursor);
        } catch (Exception e) {
            throw new OtbooException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }

    // TODO 로그인 로직 추가되면 사용 예정 - 커버리지 문제로 제거 상태
//    private static UUID currentUserId() {
//        var auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || auth.getName() == null) throw new OtbooException(ErrorCode.UNAUTHORIZED);
//        return UUID.fromString(auth.getName());
//    }


    @Transactional
    public DmEvent persistAndBuildEvent(UUID senderId, SendDmRequest request) {
        User senderRef   = em.getReference(User.class, senderId);
        User receiverRef = em.getReference(User.class, request.toUserId());

        DirectMessage entity = DirectMessage.builder()
            .sender(senderRef)
            .receiver(receiverRef)
            .message(request.content())
            .build();

        directMessageRepository.save(entity);

        return DmEvent.builder()
            .id(entity.getId())
            .senderId(senderId)
            .receiverId(request.toUserId())
            .content(entity.getMessage())
            .createdAt(entity.getCreatedAt())
            .status("SENT")
            .tempId(request.tempId())
            .build();
    }

    @Transactional
    public DmReadEvent markRead(UUID me, DmReadRequest request) {
        int updated = directMessageRepository.markAsReadBetween(me, request.peerId(), request.lastMessageId());
        return new DmReadEvent(request.peerId(), me, request.lastMessageId(), updated);
    }
}
