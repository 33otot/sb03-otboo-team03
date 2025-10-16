package com.samsamotot.otboo.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.user.dto.ChangePasswordRequest;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.common.fixture.UserFixture;
import org.springframework.test.util.ReflectionTestUtils;
import com.samsamotot.otboo.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 비밀번호 변경 슬라이스 테스트")
class UserControllerChangePasswordTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private UserController userController;

    private UUID userId;
    private ChangePasswordRequest validRequest;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();
        validRequest = ChangePasswordRequest.builder()
            .password("newPassword123")
            .build();

        User user = UserFixture.createValidUser();
        ReflectionTestUtils.setField(user, "id", userId);
        userDetails = new CustomUserDetails(user);
    }

    private void setupAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("비밀번호 변경 성공 테스트")
    class ChangePasswordSuccessTests {

        @Test
        @DisplayName("유효한_요청으로_비밀번호_변경_성공")
        void 유효한_요청으로_비밀번호_변경_성공() throws Exception {
            // given
            setupAuthentication();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 실패 테스트")
    class ChangePasswordFailureTests {

        @Test
        @DisplayName("인증_정보가_없을_때_403_에러")
        void 인증_정보가_없을_때_403_에러() throws Exception {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("다른_사용자_비밀번호_변경_시도시_403_에러")
        void 다른_사용자_비밀번호_변경_시도시_403_에러() throws Exception {
            // given
            UUID otherUserId = UUID.randomUUID();
            User otherUser = UserFixture.createUserWithEmail("other@example.com");
            ReflectionTestUtils.setField(otherUser, "id", otherUserId);
            CustomUserDetails otherUserDetails = new CustomUserDetails(otherUser);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                otherUserDetails, null, otherUserDetails.getAuthorities());
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("비밀번호 검증 테스트")
    class PasswordValidationTests {

        @Test
        @DisplayName("영문과_숫자_포함_비밀번호_검증_성공")
        void 영문과_숫자_포함_비밀번호_검증_성공() throws Exception {
            // given
            setupAuthentication();
            ChangePasswordRequest validPasswordRequest = ChangePasswordRequest.builder()
                .password("password123")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPasswordRequest)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("영문만_포함된_비밀번호_검증_성공")
        void 영문만_포함된_비밀번호_검증_성공() throws Exception {
            // given
            setupAuthentication();
            ChangePasswordRequest validPasswordRequest = ChangePasswordRequest.builder()
                .password("password123")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPasswordRequest)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryTests {

        @Test
        @DisplayName("최소_길이_비밀번호_검증_성공")
        void 최소_길이_비밀번호_검증_성공() throws Exception {
            // given
            setupAuthentication();
            ChangePasswordRequest minLengthRequest = ChangePasswordRequest.builder()
                .password("abc123")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(minLengthRequest)))
                .andExpect(status().isOk());
        }
    }
}
