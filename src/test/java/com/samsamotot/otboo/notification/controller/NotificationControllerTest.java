package com.samsamotot.otboo.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import com.samsamotot.otboo.notification.dto.NotificationRequest;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PackageName  : com.samsamotot.otboo.notification.controller
 * FileName     : NotificationControllerTest
 * Author       : dounguk
 * Date         : 2025. 9. 30.
 */
@WebMvcTest(NotificationController.class)
@DisplayName("Notification 컨트롤러 슬라이스 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void 알림목록_정상적으로_가져온다() throws Exception {
        // given
        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .receiverId(UUID.randomUUID())
            .title("테스트 알림")
            .content("내용입니다")
            .level(NotificationLevel.INFO)
            .build();

        NotificationListResponse response = NotificationListResponse.builder()
            .data(List.of(dto))
            .nextCursor(null)
            .nextIdAfter(null)
            .hasNext(false)
            .totalCount(1L)
            .sortBy("createdAt")
            .sortDirection("DESCENDING")
            .build();

        given(notificationService.getNotifications(any(NotificationRequest.class)))
            .willReturn(response);

        // when n then
        mockMvc.perform(get("/api/notifications")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("테스트 알림"))
            .andExpect(jsonPath("$.totalCount").value(1));
    }

}