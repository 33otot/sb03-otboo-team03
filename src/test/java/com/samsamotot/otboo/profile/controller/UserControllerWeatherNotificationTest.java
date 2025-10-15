package com.samsamotot.otboo.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.profile.dto.NotificationSettingUpdateRequest;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.user.controller.UserController;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityTestConfig.class)
@DisplayName("UserController 날씨 알림 설정 테스트")
class UserControllerWeatherNotificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserService userService;

    private User mockUser;
    private CustomUserDetails mockPrincipal;

    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createValidUser();
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        mockPrincipal = new CustomUserDetails(mockUser);
    }

    @Test
    @DisplayName("날씨 알림 수신 여부를 true로 변경하면 204가 반환된다")
    void 날씨_알림_수신_여부를_true로_변경하면_204가_반환된다() throws Exception {
        // given
        NotificationSettingUpdateRequest request = new NotificationSettingUpdateRequest(true);
        doNothing().when(profileService).updateNotificationEnabled(eq(mockUser.getId()), any());

        // when & then
        mockMvc.perform(patch("/api/users/profiles/notification-weathers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(user(mockPrincipal)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("날씨 알림 수신 여부를 false로 변경하면 204가 반환된다")
    void 날씨_알림_수신_여부를_false로_변경하면_204가_반환된다() throws Exception {
        // given
        NotificationSettingUpdateRequest request = new NotificationSettingUpdateRequest(false);
        doNothing().when(profileService).updateNotificationEnabled(eq(mockUser.getId()), any());

        // when & then
        mockMvc.perform(patch("/api/users/profiles/notification-weathers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(user(mockPrincipal)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("인증되지 않은 요청은 401이 반환된다")
    void 인증되지_않은_요청은_401이_반환된다() throws Exception {
        // given
        NotificationSettingUpdateRequest request = new NotificationSettingUpdateRequest(true);

        // when & then
        mockMvc.perform(patch("/api/users/profiles/notification-weathers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("request body가 null이면 400이 반환된다")
    void request_body가_null이면_400이_반환된다() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/users/profiles/notification-weathers")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user(mockPrincipal)))
            .andExpect(status().isBadRequest());
    }
}