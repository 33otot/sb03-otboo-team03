package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.DmEvent;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.dto.SendDmRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.mapper.DirectMessageMapper;
import com.samsamotot.otboo.directmessage.repository.DirectMessageRepository;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageServiceImpl implements DirectMessageService {
    private static final String DM_SERVICE = "[DirectMessageService] ";

    private final DirectMessageRepository directMessageRepository;

    private final UserRepository userRepository;

    private final DirectMessageMapper directMessageMapper;

    private final NotificationService notificationService;

    private final EntityManager em;

    @Override
    public DirectMessageListResponse getMessages(MessageRequest request) {
        log.info(DM_SERVICE + "DM 목록 조회 시작 - request: {}", request);
        UUID myId = currentUserId();
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

        Instant cursor = request.cursor();
        UUID idAfter = request.idAfter();
        int limit = Math.max(1, request.limit());

        List<DirectMessage> rows;
        if (cursor == null) {
            rows = directMessageRepository.findFirstPage(myId, otherId, PageRequest.of(0, limit + 1));
        } else {
            rows = directMessageRepository.findNextPage(myId, otherId, cursor, idAfter, PageRequest.of(0, limit + 1));
        }

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

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        log.debug(DM_SERVICE + "[DEBUG] authClass={}, name={}, principalClass={}, principal={}",
            (auth != null ? auth.getClass() : null),
            (auth != null ? auth.getName() : null),
            (auth != null && auth.getPrincipal() != null ? auth.getPrincipal().getClass() : null),
            (auth != null ? auth.getPrincipal() : null));

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            log.warn(DM_SERVICE + "인증 실패: Authentication 비어있음/미인증 (auth={}, principal={})", auth, (auth != null ? auth.getPrincipal() : null));
            throw new OtbooException(ErrorCode.UNAUTHORIZED);
        }

        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        throw new OtbooException(ErrorCode.UNAUTHORIZED);
    }


    // TODO 작업중
    @Transactional
    public DmEvent persistAndBuildEvent(UUID senderId, SendDmRequest request) {
        log.info(DM_SERVICE + "DM 저장 시작 - senderId: {}, receiverId: {}, content: {}",
            senderId, request.receiverId(), request.content());

        String content = request.content() == null ? "" : request.content();

        User senderRef   = em.getReference(User.class, senderId);
        User receiverRef = em.getReference(User.class, request.receiverId());

        DirectMessage entity = DirectMessage.builder()
            .sender(senderRef)
            .receiver(receiverRef)
            .message(content)
            .build();

        DirectMessage savedEntity = directMessageRepository.save(entity);
        log.info(DM_SERVICE + "DM 저장 완료 - id: {}, createdAt: {}",
            savedEntity.getId(), savedEntity.getCreatedAt());

        String notificationContent = request.content();
        if (notificationContent.length() > 10) {
            notificationContent = request.content().substring(0, 10) + "...";
        }

        notificationService.notifyDirectMessage(senderId,request.receiverId(), notificationContent);

        return DmEvent.builder()
            .id(savedEntity.getId())
            .senderId(senderId)
            .receiverId(request.receiverId())
            .content(savedEntity.getMessage())
            .createdAt(savedEntity.getCreatedAt())
            .status("DELIVERED")
            .tempId(UUID.randomUUID())
            .build();
    }

}
