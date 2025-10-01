package com.samsamotot.otboo.notification.controller;

import com.samsamotot.otboo.notification.controller.api.NotificationApi;
import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import com.samsamotot.otboo.notification.dto.NotificationRequest;
import com.samsamotot.otboo.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.controller
 * FileName     : NotificationController
 * Author       : dounguk
 * Date         : 2025. 9. 30.
 * Description  : 알림 관련 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("api/notifications")
public class NotificationController implements NotificationApi {
    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     *
     * @param cursor   조회 기준 시각 (이전 알림 조회, optional)
     * @param idAfter  동일 시각에서의 타이브레이커 ID (optional)
     * @param limit    조회할 데이터 개수 (필수)
     * @return NotificationListResponse (알림 목록 + 페이지네이션 정보)
     */
    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit
    ) {
        NotificationRequest request = new NotificationRequest(cursor, idAfter, limit);
        return ResponseEntity.ok().body(notificationService.getNotifications(request));
    }
}
