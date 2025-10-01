package com.samsamotot.otboo.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import com.samsamotot.otboo.notification.dto.NotificationRequest;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.repository.NotificationRepository;
import com.samsamotot.otboo.sse.service.SseService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
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
 * PackageName  : com.samsamotot.otboo.notification.service
 * FileName     : NotificationServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {
    private static final String NOTIFICATION_SERVICE = "[NotificationService] ";

    private static final String ROLE_TITLE = "권한 변경";
    private static final String CLOTHES_ATTRBUTE_TITLE = "의상 속성 추가";
    private static final String LIKE_TITLE = "새 좋아요";
    private static final String COMMENT_TITLE = "새 댓글";
    private static final String FOLLOW_TITLE = "새 팔로워";
    private static final String DIRECT_MESSAGE_TITLE = "새 쪽지";

    private static final String ROLE_CONTENT = "변경된 권한을 확인하세요";
    private static final String CLOTHES_ATTRBUTE_CONTENT = "의상 속성이 추가되었습니다.";
    private static final String LIKE_CONTENT = "from: ";
    private static final String FOLLOW_CONTENT = "사용자가 팔로우했습니다";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    /**
     * 알림을 저장하고 해당 사용자에게 SSE로 실시간 발행한다.
     * DB 저장은 보장되며, SSE 발행 실패 시에도 저장은 유지된다.
     *
     * @param receiverId 알림을 받을 사용자 ID
     * @param title 알림 제목
     * @param content 알림 내용
     * @param level 알림 수준 (INFO, WARN, ERROR 등)
     * @return 저장된 Notification 객체
     */
    @Transactional
    public Notification save(UUID receiverId, String title, String content, NotificationLevel level) {

        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> {
                log.warn(NOTIFICATION_SERVICE + "인증 사용자 조회 실패: receiverId={} (DB에 없음)", receiverId);
                return new OtbooException(ErrorCode.UNAUTHORIZED);
            });

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title(title)
            .content(content)
            .level(level)
            .build();

        Notification saved = notificationRepository.save(notification);

        // SSE로 실시간 발행
        try {
            String notificationJson = objectMapper.writeValueAsString(toDto(saved));
            sseService.sendNotification(receiverId, notificationJson);
            log.info(NOTIFICATION_SERVICE+" SSE 보냄 user: {}, title: '{}'", receiverId, title);
        } catch (Exception e) {
            log.error(NOTIFICATION_SERVICE + "SSE 발행 실패해도 알림 저장은 성공했으므로 계속 진행 user: {}", receiverId, e);
        }

        return saved;
    }

    // 다른 비즈니스 로직에서의 알람
    /**
     * 사용자 권한 변경 시 알림을 발행한다.
     */
    @Transactional
    public void notifyRole(UUID userId) {
        save(userId, ROLE_TITLE, ROLE_CONTENT, NotificationLevel.INFO);
    }

    /**
     * 의상 속성 추가 시 알림을 발행한다.
     */
    @Transactional
    public void notifyClothesAttrbute(UUID userId) {
        save(userId, CLOTHES_ATTRBUTE_TITLE, CLOTHES_ATTRBUTE_CONTENT, NotificationLevel.INFO);
    }

    /**
     * 피드에 좋아요가 추가되었을 때 피드 소유자에게 알림을 발행한다.
     */
    @Transactional
    public void notifyLike(UUID commenterId, UUID feedOwnerId) {
        save(feedOwnerId, LIKE_TITLE, LIKE_CONTENT + commenterId, NotificationLevel.INFO);
    }

    /**
     * 피드에 댓글이 달렸을 때 피드 소유자에게 알림을 발행한다.
     */
    @Transactional
    public void notifyComment(UUID commenterId, UUID feedOwnerId, String commentPreview) {
        save(feedOwnerId, COMMENT_TITLE, "from: " + commenterId + ", content: " + commentPreview, NotificationLevel.INFO);
    }

    /**
     * 새로운 팔로워가 생겼을 때 팔로우 당한 사용자에게 알림을 발행한다.
     */
    @Transactional
    public void notifyFollow(UUID followerId, UUID followeeId) {
        save(followeeId, FOLLOW_TITLE, FOLLOW_CONTENT, NotificationLevel.INFO);
    }

    /**
     * 새로운 쪽지가 도착했을 때 수신자에게 알림을 발행한다.
     */
    @Transactional
    public void notifyDirectMessage(UUID senderId, UUID receiverId, String messagePreview) {
        save(receiverId, DIRECT_MESSAGE_TITLE, "from: " + senderId + ", content: " + messagePreview, NotificationLevel.INFO);
    }

    /**
     * 현재 로그인한 사용자의 알림 목록을 커서 기반으로 조회한다.
     *
     * - 최신 알림부터 limit 개수만큼 반환한다.
     * - 커서(timestamp, idAfter)가 주어지면 해당 위치 이후(더 과거) 알림을 조회한다.
     * - limit+1 개를 조회하여 hasNext 여부를 판단한다.
     * - 응답에는 다음 페이지를 위한 nextCursor, nextIdAfter 정보를 포함한다.
     *
     * @param request 알림 조회 요청(cursor, idAfter, limit)
     * @return 알림 목록 응답 DTO (데이터, 커서, hasNext, totalCount 등)
     */
    @Override
    public NotificationListResponse getNotifications(NotificationRequest request) {
        UUID receiverId = currentUserId();

        int limit = Math.max(1, request.limit());

        PageRequest pageRequest = PageRequest.of(0, limit+1);

        List<Notification> rows;
        if (request.cursor() == null) {
            log.info("첫번째 조회(커서 값 null)");
            rows = notificationRepository.findLatest(receiverId, pageRequest);
        } else {
            log.info("두번째 조회(커서 값 있음)");
            rows = notificationRepository.findAllBefore(
                receiverId,
                request.cursor(),
                request.idAfter(),
                pageRequest
            );
        }

        boolean hasNext = rows.size() > limit;
        List<Notification> pageRows = hasNext ? rows.subList(0, limit) : rows;

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (!pageRows.isEmpty()) {
            Notification last = pageRows.get(pageRows.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        long total = notificationRepository.countByReceiver_Id(receiverId);

        List<NotificationDto> data = pageRows.stream().map(this::toDto).toList();

        log.info(NOTIFICATION_SERVICE + "DM 목록 조회 완료 - total: {}, hasNext: {}", total, hasNext);

        return NotificationListResponse.builder()
            .data(data)
            .nextCursor(hasNext ? nextCursor : null)
            .nextIdAfter(hasNext ? nextIdAfter : null)
            .hasNext(hasNext)
            .totalCount(total)
            .sortBy("createdAt")
            .sortDirection("DESCENDING")
            .build();
    }

    /*
        로그인 유저 아이디를 가져온다.
     */
    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        log.debug(NOTIFICATION_SERVICE + "[DEBUG] authClass={}, name={}, principalClass={}, principal={}",
            (auth != null ? auth.getClass() : null),
            (auth != null ? auth.getName() : null),
            (auth != null && auth.getPrincipal() != null ? auth.getPrincipal().getClass() : null),
            (auth != null ? auth.getPrincipal() : null));

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            log.warn(NOTIFICATION_SERVICE + "인증 실패: Authentication 비어있음/미인증 (auth={}, principal={})", auth, (auth != null ? auth.getPrincipal() : null));
            throw new OtbooException(ErrorCode.UNAUTHORIZED);
        }

        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        throw new OtbooException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * Notification 엔티티를 전송용 DTO로 변환한다.
     */
    private NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
            .id(notification.getId())
            .createdAt(notification.getCreatedAt())
            .receiverId(notification.getReceiver().getId())
            .title(notification.getTitle())
            .content(notification.getContent())
            .level(notification.getLevel())
            .build();
    }
}