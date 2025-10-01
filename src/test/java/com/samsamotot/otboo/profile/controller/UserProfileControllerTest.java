package com.samsamotot.otboo.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.ProfileFixture;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.profile.entity.Gender;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.user.controller.UserController;
import com.samsamotot.otboo.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    @DisplayName("유저 프로필 조회 (GET /api/users/{userId}/profiles)")
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

    @Nested
    @DisplayName("유저 프로필 수정 (PATCH /api/users/{userId}/profiles)")
    class UpdateProfileTests {
        @Test
        void 이미지_있는_프로필_수정_성공하면_200_DTO() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            String profileImageUrl = "https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/new-image.png";

            ProfileUpdateRequest requestDto = ProfileUpdateRequest.builder()
                    .name("수정된 이름")
                    .gender(Gender.FEMALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .location(LocationFixture.createLocation())
                    .temperatureSensitivity(4.5)
                    .build();

            String requestDtoJson = objectMapper.writeValueAsString(requestDto);

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request",
                    "",
                    "application/json",
                    requestDtoJson.getBytes(StandardCharsets.UTF_8)
            );

            MockMultipartFile profileImageFile = new MockMultipartFile(
                    "profileImageUrl",
                    "profile.png",
                    MediaType.IMAGE_PNG_VALUE,
                    profileImageUrl.getBytes()
            );

            ProfileDto updatedResultDto = ProfileDto.builder()
                    .userId(userId)
                    .locationId(requestDto.location().getId())
                    .gender(requestDto.gender())
                    .birthDate(requestDto.birthDate())
                    .temperatureSensitivity(requestDto.temperatureSensitivity())
                    .build();

            given(profileService.updateProfile(eq(userId), any(ProfileUpdateRequest.class), any(MultipartFile.class)))
                    .willReturn(updatedResultDto);

            // When
            // Then
            mockMvc.perform(
                    multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
                            .file(profileImageFile)
                            .file(requestPart)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(updatedResultDto.userId().toString()));
        }

        @Test
        void 이미지_없는_프로필_수정_성공하면_200_DTO() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            ProfileUpdateRequest requestDto = ProfileUpdateRequest.builder()
                    .name("수정된 이름")
                    .gender(Gender.FEMALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .location(LocationFixture.createLocation())
                    .temperatureSensitivity(4.5)
                    .build();

            String requestDtoJson = objectMapper.writeValueAsString(requestDto);

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request",
                    "",
                    "application/json",
                    requestDtoJson.getBytes(StandardCharsets.UTF_8)
            );

            ProfileDto updatedResultDto = ProfileDto.builder()
                    .userId(userId)
                    .name(requestDto.name())
                    .gender(requestDto.gender())
                    .birthDate(requestDto.birthDate())
                    .temperatureSensitivity(requestDto.temperatureSensitivity())
                    .profileImageUrl(null)
                    .build();

            given(profileService.updateProfile(eq(userId), any(ProfileUpdateRequest.class), any(MultipartFile.class.)))
                    .willReturn(updatedResultDto);

            // When
            // Then
            mockMvc.perform(
                    multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
                            .file(requestPart)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.name").value(updatedResultDto.name()))
                    .andExpect(jsonPath("$.profileImageUrl").isEmpty());
        }

        @Test
        void 잘못된_입력값으로_프로필_수정_실패하면_400_BAD_REQUEST() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            ProfileUpdateRequest requestDto = ProfileUpdateRequest.builder()
                    .name("수정할이름")
                    .gender(Gender.MALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .temperatureSensitivity(6.0) // 유효 범위를 벗어난 잘못된 값 (1.0 ~ 5.0)
                    .build();

            String requestJson = objectMapper.writeValueAsString(requestDto);

            // When
            ResultActions actions = mockMvc.perform(
                    patch("/api/users/{userId}/profiles", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .contentType(requestJson)
            );

            // Then
            actions.andExpect(status().isBadRequest());
        }
    }
}
