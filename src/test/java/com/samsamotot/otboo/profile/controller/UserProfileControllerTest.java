package com.samsamotot.otboo.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.ProfileFixture;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.user.controller.UserController;
import com.samsamotot.otboo.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController 프로필 API 테스트")
public class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean // @WebMvcTest의 컨트롤러가 의존하는 가짜 빈
    private UserService userService;

    @MockitoBean
    private ProfileService profileService;

    @Nested
    @DisplayName("사용자 프로필 조회 (GET /api/users/{userId}/profiles)")
    class GetProfileTests {

        @Test
        void 존재하는_사용자_조회하면_200_DTO() throws Exception {
            // Given
            Profile profile = ProfileFixture.perfectProfileWithNoImage();

            UUID userId = UUID.randomUUID();

            ProfileDto result = ProfileDto.builder()
                    .userId(userId)
                    .locationId(profile.getLocation().getId())
                    .name(profile.getName())
                    .gender(profile.getGender())
                    .birthDate(profile.getBirthDate())
                    .temperatureSensitivity(profile.getTemperatureSensitivity())
                    .profileImageUrl(profile.getProfileImageUrl())
                    .build();

            given(profileService.getProfileByUserId(userId))
                    .willReturn(result);

            // When
            // Then
            mockMvc.perform(get("/api/users/{userId}/profiles", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.name").value(result.name()))
                    .andExpect(jsonPath("$.gender").value(result.gender().name()))
                    .andExpect(jsonPath("$.birthDate").value(result.birthDate().toString()))
                    .andExpect(jsonPath("$.temperatureSensitivity").value(result.temperatureSensitivity()))
                    .andExpect(jsonPath("$.profileImageUrl").value(result.profileImageUrl()));
        }

        @Test
        void 존재하지_않는_사용자로_조회하면_404_NOT_FOUND() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            given(profileService.getProfileByUserId(userId))
                    .willThrow(new OtbooException(ErrorCode.USER_NOT_FOUND));

            // When
            // Then
            mockMvc.perform(get("/api/users/{userId}/profiles", userId))
                    .andExpect(status().isNotFound())
                    .andDo(print());
        }
    }
}
