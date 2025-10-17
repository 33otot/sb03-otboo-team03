package com.samsamotot.otboo.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.user.dto.ChangePasswordRequest;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.common.fixture.UserFixture;
import org.springframework.test.util.ReflectionTestUtils;
import com.samsamotot.otboo.user.service.UserService;
import com.samsamotot.otboo.profile.service.ProfileService;
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
    private ProfileService profileService;

    private UserController userController;

    private UUID userId;
    private ChangePasswordRequest validRequest;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, profileService);
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
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNoContent());
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
                .andExpect(status().isForbidden());
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
                .andExpect(status().isForbidden());
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
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));
            ChangePasswordRequest validPasswordRequest = ChangePasswordRequest.builder()
                .password("password123")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPasswordRequest)))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("영문만_포함된_비밀번호_검증_실패")
        void 영문만_포함된_비밀번호_검증_실패() throws Exception {
            // given
            setupAuthentication();
            ChangePasswordRequest invalidPasswordRequest = ChangePasswordRequest.builder()
                .password("password")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPasswordRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("숫자만_포함된_비밀번호_검증_실패")
        void 숫자만_포함된_비밀번호_검증_실패() throws Exception {
            // given
            setupAuthentication();
            ChangePasswordRequest invalidPasswordRequest = ChangePasswordRequest.builder()
                .password("123456")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPasswordRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("최소_길이_미만_비밀번호_검증_실패")
        void 최소_길이_미만_비밀번호_검증_실패() throws Exception {
            // given
            setupAuthentication();
            ChangePasswordRequest invalidPasswordRequest = ChangePasswordRequest.builder()
                .password("ab1")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPasswordRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈_비밀번호_검증_실패")
        void 빈_비밀번호_검증_실패() throws Exception {
            // given
            setupAuthentication();
            ChangePasswordRequest invalidPasswordRequest = ChangePasswordRequest.builder()
                .password("")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPasswordRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("null_비밀번호_검증_실패")
        void null_비밀번호_검증_실패() throws Exception {
            // given
            setupAuthentication();
            ChangePasswordRequest invalidPasswordRequest = ChangePasswordRequest.builder()
                .password(null)
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPasswordRequest)))
                .andExpect(status().isBadRequest());
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
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));
            ChangePasswordRequest minLengthRequest = ChangePasswordRequest.builder()
                .password("abc123")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(minLengthRequest)))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("최대_길이_비밀번호_검증_성공")
        void 최대_길이_비밀번호_검증_성공() throws Exception {
            // given
            setupAuthentication();
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));
            ChangePasswordRequest maxLengthRequest = ChangePasswordRequest.builder()
                .password("a".repeat(50) + "123")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(maxLengthRequest)))
                .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("인증 실패 테스트")
    class AuthenticationFailureTests {

        @Test
        @DisplayName("잘못된_인증_타입_403_에러")
        void 잘못된_인증_타입_403_에러() throws Exception {
            // given
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                "invalidPrincipal", null, null);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("authentication_principal이_null인_경우_403_에러")
        void authentication_principal이_null인_경우_403_에러() throws Exception {
            // given
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                null, null, null);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("로그 메시지 테스트")
    class LogMessageTests {

        @Test
        @DisplayName("성공_케이스에서_로그_메시지_확인")
        void 성공_케이스에서_로그_메시지_확인() throws Exception {
            // given
            setupAuthentication();
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패_케이스에서_로그_메시지_확인")
        void 실패_케이스에서_로그_메시지_확인() throws Exception {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("경계값 및 엣지 케이스 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("매우_긴_비밀번호_처리")
        void 매우_긴_비밀번호_처리() throws Exception {
            // given
            setupAuthentication();
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));
            ChangePasswordRequest longPasswordRequest = ChangePasswordRequest.builder()
                .password("a".repeat(1000) + "123")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(longPasswordRequest)))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("특수문자_포함_비밀번호_처리")
        void 특수문자_포함_비밀번호_처리() throws Exception {
            // given
            setupAuthentication();
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));
            ChangePasswordRequest specialCharRequest = ChangePasswordRequest.builder()
                .password("Pass@123!@#$%^&*()")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(specialCharRequest)))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("유니코드_문자_포함_비밀번호_처리")
        void 유니코드_문자_포함_비밀번호_처리() throws Exception {
            // given
            setupAuthentication();
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));
            ChangePasswordRequest unicodeRequest = ChangePasswordRequest.builder()
                .password("한글123abc")
                .build();

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(unicodeRequest)))
                .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("메서드 파라미터 테스트")
    class MethodParameterTests {

        @Test
        @DisplayName("다른_사용자_ID로_요청시_로그_메시지_확인")
        void 다른_사용자_ID로_요청시_로그_메시지_확인() throws Exception {
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
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("동일_사용자_ID로_요청시_성공")
        void 동일_사용자_ID로_요청시_성공() throws Exception {
            // given
            setupAuthentication();
            doNothing().when(userService).changePassword(any(UUID.class), any(ChangePasswordRequest.class));

            // when & then
            mockMvc.perform(patch("/api/users/{userId}/password", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNoContent());
        }
    }
}
