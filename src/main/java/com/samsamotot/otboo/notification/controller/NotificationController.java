package com.samsamotot.otboo.notification.controller;

import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.notification.controller.api.NotificationApi;
import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import com.samsamotot.otboo.notification.dto.NotificationRequest;
import com.samsamotot.otboo.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.controller
 * FileName     : NotificationController
 * Author       : dounguk
 * Date         : 2025. 9. 30.
 * Description  : 알림 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("api/notifications")
public class NotificationController implements NotificationApi {
    private static final String NOTIFICATION_CONTROLLER = "[NotificationController]";

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     *
     * @param cursor   조회 기준 시각 (이전 알림 조회, optional)
     * @param idAfter  동일 시각에서의 타이브레이커 ID (optional)
     * @param limit    조회할 데이터 개수 (필수)
     * @return NotificationListResponse (알림 목록 + 페이지네이션 정보)
     */
    @Override
    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit
    ) {
        NotificationRequest request = new NotificationRequest(cursor, idAfter, limit);
        return ResponseEntity.ok().body(notificationService.getNotifications(request));
    }

    /**
     * 특정 알림(notification)을 삭제한다.
     * <p>알림 ID를 경로 변수로 받아 {@link NotificationService}를 통해 삭제를 수행한다.
     * 삭제가 완료되면 HTTP 204 No Content 응답을 반환한다.</p>
     *
     * @param notificationId 삭제할 알림의 식별자(UUID)
     * @return 204 No Content
     */
    @Override
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotifications(@PathVariable UUID notificationId) {
        notificationService.delete(notificationId);
        log.info(NOTIFICATION_CONTROLLER + "알람이 삭제 되었습니다.: {}", notificationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteAllByUserId(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getId();
        notificationService.deleteAllByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
