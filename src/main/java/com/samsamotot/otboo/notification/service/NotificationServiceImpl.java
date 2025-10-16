package com.samsamotot.otboo.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.service
 * FileName     : NotificationServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Slf4j
@Validated
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    private static final String NOTIFICATION_SERVICE = "[NotificationService] ";

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
            log.info(NOTIFICATION_SERVICE + " SSE 보냄 user: {}, title: '{}'", receiverId, title);
        } catch (Exception e) {
            log.error(NOTIFICATION_SERVICE + "SSE 발행 실패해도 알림 저장은 성공했으므로 계속 진행 user: {}", receiverId, e);
        }
        return saved;
    }

    /**
     * 활성 사용자(잠금되지 않은 사용자)에게만 배치로 알림을 저장하고 SSE로 실시간 발행한다.
     * 성능 최적화: 한 번의 트랜잭션으로 배치 저장 후 비동기 SSE 발행
     *
     * @param title 알림 제목
     * @param content 알림 내용
     * @param level 알림 수준 (INFO, WARN, ERROR 등)
     */
    @Transactional
    @Override
    public void saveBatchNotification(String title, String content, NotificationLevel level) {
        log.info(NOTIFICATION_SERVICE + "배치 알림 저장 시작 - title: '{}', level: {}", title, level);

        // 활성 사용자 ID만 조회
        List<UUID> userIds = userRepository.findActiveUserIds();
        log.info(NOTIFICATION_SERVICE + "총 {}명의 활성 사용자에게 알림 발송", userIds.size());

        if (userIds.isEmpty()) {
            log.info(NOTIFICATION_SERVICE + "활성 사용자가 없어 알림 발송을 건너뜁니다");
            return;
        }

        // 1. 배치로 알림 저장
        List<Notification> savedNotifications = saveBatchNotifications(userIds, title, content, level);
        log.info(NOTIFICATION_SERVICE + "배치 알림 저장 완료 - {}건 저장", savedNotifications.size());

        // 2. 트랜잭션 커밋 후 SSE 발행
        TransactionSynchronizationManager
            .registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendBatchSseNotifications(savedNotifications);
                }
            });
    }
    
    /**
     * 사용자 목록에 대해 배치로 알림을 저장한다.
     * 
     * @param userIds 사용자 ID 목록
     * @param title 알림 제목
     * @param content 알림 내용
     * @param level 알림 수준
     * @return 저장된 알림 목록
     */
    @Transactional
    protected List<Notification> saveBatchNotifications(List<UUID> userIds, String title, String content, NotificationLevel level) {

        List<User> users = userRepository.findAllById(userIds);

        List<Notification> notifications = users.stream()
            .map(user -> Notification.builder()
                .receiver(user)
                .title(title)
                .content(content)
                .level(level)
                .build())
            .toList();

        // 배치 저장
        return notificationRepository.saveAll(notifications);
    }
    
    /**
     * 저장된 알림 목록에 대해 SSE로 실시간 발행한다.
     * 분산 환경을 고려하여 배치 전송을 사용한다.
     * 
     * @param notifications 발행할 알림 목록
     */
    protected void sendBatchSseNotifications(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        // 첫 번째 알림을 기준으로 배치 전송 (모든 알림이 동일한 내용이라고 가정)
        Notification firstNotification = notifications.get(0);
        try {
            String notificationJson = objectMapper.writeValueAsString(toDto(firstNotification));
            
            // 사용자 ID 목록 추출
            List<UUID> userIds = notifications.stream()
                .map(notification -> notification.getReceiver().getId())
                .toList();
            
            // 분산 SSE 배치 전송 사용
            sseService.sendBatchNotification(userIds, notificationJson);
            
            log.info(NOTIFICATION_SERVICE + "분산 SSE 배치 발행 완료 - {}건 발행", notifications.size());
        } catch (Exception e) {
            log.error(NOTIFICATION_SERVICE + "분산 SSE 배치 발행 실패", e);
            
            // 배치 전송 실패 시 개별 전송으로 폴백
            notifications.forEach(notification -> {
                try {
                    String notificationJson = objectMapper.writeValueAsString(toDto(notification));
                    sseService.sendNotification(notification.getReceiver().getId(), notificationJson);
                    log.debug(NOTIFICATION_SERVICE + "개별 SSE 발행 완료 - user: {}, title: '{}'", 
                        notification.getReceiver().getId(), notification.getTitle());
                } catch (Exception ex) {
                    log.error(NOTIFICATION_SERVICE + "개별 SSE 발행 실패 - user: {}, title: '{}'", 
                        notification.getReceiver().getId(), notification.getTitle(), ex);
                }
            });
        }
    }

    /**
     * 현재 로그인 사용자의 알림 목록을 커서 기반으로 조회한다.
     *
     * - 정렬: createdAt DESC, id DESC (최신순)
     * - 첫 페이지: cursor/idAfter 없이 limit만 전달
     * - 다음 페이지: 응답의 nextCursor/nextIdAfter를 그대로 사용
     * - 서버는 limit+1개를 조회해 hasNext를 판단하고, hasNext=true일 때만
     *   nextCursor/nextIdAfter를 채워준다.
     *
     * @param request cursor(옵션), idAfter(옵션), limit(필수; 최소 1로 보정)
     * @return NotificationListResponse (data, hasNext, nextCursor, nextIdAfter, totalCount 등)
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

    /**
     * 주어진 ID의 알림(notification)을 삭제한다.
     * <p>로그를 남기고, {@link NotificationRepository}를 통해 실제 DB에서 삭제를 수행한다.</p>
     *
     * @param notificationId 삭제할 알림의 식별자(UUID)
     */
    @Transactional
    @Override
    public void delete(UUID notificationId) {
        UUID currentUserId = currentUserId();

        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> {
                log.warn(NOTIFICATION_SERVICE + "알림을 찾을 수 없음: notificationId={}", notificationId);
                return new OtbooException(ErrorCode.NOTIFICATION_NOT_FOUND);
            });

        if (!notification.getReceiver().getId().equals(currentUserId)) {
            log.warn(NOTIFICATION_SERVICE + "권한 없음: userId={}, notificationOwnerId={}",
                currentUserId, notification.getReceiver().getId());
            throw new OtbooException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationRepository.delete(notification);
        log.info(NOTIFICATION_SERVICE + "삭제 완료: notificationId={}, userId={}", notificationId, currentUserId);
    }

    /*
        로그인 유저 아이디를 가져온다.
     */
    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

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