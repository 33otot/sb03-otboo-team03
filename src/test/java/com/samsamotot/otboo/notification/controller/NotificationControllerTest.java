package com.samsamotot.otboo.notification.controller;

import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.config.TestConfig;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import com.samsamotot.otboo.notification.dto.NotificationRequest;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PackageName  : com.samsamotot.otboo.notification.controller
 * FileName     : NotificationControllerTest
 * Author       : dounguk
 * Date         : 2025. 9. 30.
 */
@Import(SecurityTestConfig.class)
@WebMvcTest(NotificationController.class)
@DisplayName("Notification 컨트롤러 슬라이스 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    User mockUser;
    CustomUserDetails mockPrincipal;

    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createValidUser();
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        mockPrincipal = new CustomUserDetails(mockUser);
    }

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
                        .param("limit", "10")
                        .with(user(mockPrincipal))
                )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("테스트 알림"))
            .andExpect(jsonPath("$.totalCount").value(1));
    }
    
    @Test
    void 읽음_요청_알림_삭제한다() throws Exception {
        // given
        UUID id = UUID.randomUUID();

        // when n then
        mockMvc.perform(delete("/api/notifications/{notificationId}", id)
                        .with(user(mockPrincipal))
                )
            .andExpect(status().isNoContent());

        then(notificationService).should().delete(id);
    }

    @Test
    void 사용자의_모든_알림_삭제() throws Exception {
        // given
        doNothing().when(notificationService).deleteAllByUserId(any(UUID.class));

        // when n then
        mockMvc.perform(delete("/api/notifications")
                        .with(user(mockPrincipal))
                        .with(csrf())
                )
                .andExpect(status().isNoContent());

        // then
        verify(notificationService).deleteAllByUserId(mockUser.getId());
    }

}