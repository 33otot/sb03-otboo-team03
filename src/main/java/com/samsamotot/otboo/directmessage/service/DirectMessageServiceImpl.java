package com.samsamotot.otboo.directmessage.service;

import static java.util.function.UnaryOperator.identity;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomDto; // Added
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomListResponse; // Added
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.dto.SendDmRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.mapper.DirectMessageMapper;
import com.samsamotot.otboo.directmessage.repository.DirectMessageRepository;
import com.samsamotot.otboo.notification.dto.event.DirectMessageReceivedEvent;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
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

    private final ProfileRepository profileRepository;

    private final DirectMessageMapper directMessageMapper;

    private final ApplicationEventPublisher eventPublisher;

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

    /**
     * 현재 사용자의 모든 대화방 목록을 조회하는 서비스 메서드.
     *
     * <p>흐름:
     * 1. 현재 사용자 ID를 인증 컨텍스트에서 조회한다.
     * 2. DirectMessageRepository를 통해 대화방 목록을 조회한다.
     * 3. 각 대화방에 대해 상대방 정보와 마지막 메시지를 포함한 DTO로 변환한다.
     * 4. 변환된 DTO 리스트를 응답 객체에 담아 반환한다.
     *
     * @return 대화방 목록 및 마지막 메시지 정보를 담은 응답 객체
     */
    @Override
    public DirectMessageRoomListResponse getConversationList() {
        UUID myId = currentUserId();
        log.info(DM_SERVICE + "대화방 목록 조회 시작 - userId: {}", myId);

        // 마지막 메시지 ID 목록
        List<UUID> lastMessageIds = Optional
            .ofNullable(directMessageRepository.findLastMessageIdsOfConversations(myId))
            .orElseGet(List::of);

        if (lastMessageIds.isEmpty()) {
            return new DirectMessageRoomListResponse(List.of());
        }

        // 메시지 + 유저 로드 (JOIN FETCH)
        List<DirectMessage> conversations = Optional
            .ofNullable(directMessageRepository.findWithUsersByIds(lastMessageIds))
            .orElseGet(List::of);

        if (conversations.isEmpty()) {
            return new DirectMessageRoomListResponse(List.of());
        }

        // 대화 상대 userId 수집
        Set<UUID> partnerIds = conversations.stream()
            .map(dm -> {
                User s = dm.getSender();
                User r = dm.getReceiver();
                UUID sid = (s != null ? s.getId() : null);
                UUID rid = (r != null ? r.getId() : null);
                return Objects.equals(sid, myId) ? rid : sid;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        // 프로필 배치 조회
        List<Profile> profiles = Optional
            .ofNullable(profileRepository.findByUserIdIn(partnerIds))
            .orElseGet(List::of);

        // userId -> profileImageUrl 맵 구성
        Map<UUID, String> profileUrlMap = partnerIds.stream()
            .collect(Collectors.toMap(identity(), id -> ""));

        for (Profile p : profiles) {
            if (p != null && p.getUser() != null && p.getUser().getId() != null) {
                profileUrlMap.put(p.getUser().getId(),
                    Optional.ofNullable(p.getProfileImageUrl()).orElse(""));
            }
        }

        List<DirectMessageRoomDto> rooms = conversations.stream()
            .map(dm -> {
                User partner = (dm.getSender() != null && myId.equals(dm.getSender().getId()))
                    ? dm.getReceiver()
                    : dm.getSender();

                // partner가 null일 수 있는 이슈 방어
                if (partner == null) {
                    log.warn("[DM_SERVICE] partner is null. dmId={}", dm.getId());
                    return null;
                }
                return directMessageMapper.toRoomDto(partner, dm, profileUrlMap);
            })
            .filter(Objects::nonNull)
            .toList();

        log.info(DM_SERVICE + "대화방 목록 조회 완료 - rooms count: {}", rooms.size());
        return new DirectMessageRoomListResponse(rooms);
    }

    /**
     * DirectMessage 엔티티를 생성·저장하고 알림까지 발송하는 서비스 메서드.
     *
     * <p>흐름:
     * 1. 송신자/수신자 엔티티 레퍼런스를 조회한다.
     * 2. DirectMessage 엔티티를 생성해 DB에 저장한다.
     * 3. 알림(Notification)을 발송하기 위해 내용을 10자 이내로 잘라 전달한다.
     * 4. 저장된 엔티티를 DTO로 변환해 반환한다.
     *
     * @param senderId 송신자 사용자 ID
     * @param request  메시지 전송 요청 (수신자 ID, 내용)
     * @return 저장된 DirectMessage를 DTO로 변환한 객체
     */
    @Override
    @Transactional
    public DirectMessageDto sendMessage(UUID senderId, SendDmRequest request) {
        log.info(DM_SERVICE + "DM 전송 시작 - senderId: {}, receiverId: {}, content: {}",
            senderId, request.receiverId(), request.content());

        String content = request.content() == null ? "" : request.content();

        User sender   = em.getReference(User.class, senderId);
        User receiver = em.getReference(User.class, request.receiverId());

        DirectMessage directMessage = DirectMessage.builder()
            .sender(sender)
            .receiver(receiver)
            .message(content)
            .build();

        DirectMessage savedEntity = directMessageRepository.save(directMessage);
        log.info(DM_SERVICE + "DM 저장 완료 - id: {}, createdAt: {}",
            savedEntity.getId(), savedEntity.getCreatedAt());

        eventPublisher.publishEvent(new DirectMessageReceivedEvent(senderId,request.receiverId(), content));

        DirectMessageDto response = directMessageMapper.toDto(savedEntity);
        log.info(DM_SERVICE + "DM 전송 완료 - id: {}", response.id());
        return response;
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
}
