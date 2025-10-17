package com.samsamotot.otboo.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.dto.UserLockRequest;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.common.fixture.UserFixture;
import org.springframework.test.util.ReflectionTestUtils;
import com.samsamotot.otboo.user.service.UserService;
import com.samsamotot.otboo.profile.service.ProfileService;
import java.time.Instant;
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

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 계정 잠금 슬라이스 테스트")
class UserControllerLockTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private ProfileService profileService;

    private UserController userController;

    private UUID userId;
    private UserLockRequest lockRequest;
    private UserLockRequest unlockRequest;
    private CustomUserDetails adminUserDetails;
    private UserDto lockedUserDto;
    private UserDto unlockedUserDto;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, profileService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();
        lockRequest = new UserLockRequest(true);
        unlockRequest = new UserLockRequest(false);

        // 관리자 사용자 설정
        User adminUser = UserFixture.createUserWithEmail("admin@example.com");
        ReflectionTestUtils.setField(adminUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(adminUser, "role", Role.ADMIN);
        adminUserDetails = new CustomUserDetails(adminUser);

        // 잠긴 사용자 DTO
        lockedUserDto = UserDto.builder()
            .id(userId)
            .createdAt(Instant.now())
            .email("user@example.com")
            .name("Test User")
            .role(Role.USER)
            .locked(true)
            .build();

        // 잠금 해제된 사용자 DTO
        unlockedUserDto = UserDto.builder()
            .id(userId)
            .createdAt(Instant.now())
            .email("user@example.com")
            .name("Test User")
            .role(Role.USER)
            .locked(false)
            .build();
    }

    private void setupAdminAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            adminUserDetails, null, adminUserDetails.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("계정 잠금 성공 테스트")
    class LockAccountSuccessTests {

        @Test
        @DisplayName("계정_잠금_성공")
        void 계정_잠금_성공() throws Exception {
        // given
        setupAdminAuthentication();
        when(userService.updateUserLockStatus(any(UUID.class), any(UserLockRequest.class)))
            .thenReturn(lockedUserDto);

        // when & then
        mockMvc.perform(patch("/api/users/{userId}/lock", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.locked").value(true));
        }

        @Test
        @DisplayName("계정_잠금_해제_성공")
        void 계정_잠금_해제_성공() throws Exception {
        // given
        setupAdminAuthentication();
        when(userService.updateUserLockStatus(any(UUID.class), any(UserLockRequest.class)))
            .thenReturn(unlockedUserDto);

        // when & then
        mockMvc.perform(patch("/api/users/{userId}/lock", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unlockRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.locked").value(false));
        }
    }

    @Nested
    @DisplayName("계정 잠금 실패 테스트")
    class LockAccountFailureTests {

        @Test
        @DisplayName("잘못된_요청_데이터_400_에러")
        void 잘못된_요청_데이터_400_에러() throws Exception {
        // given
        setupAdminAuthentication();
        String invalidJson = "{\"locked\": \"invalid\"}";

        // when & then
        mockMvc.perform(patch("/api/users/{userId}/lock", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잠금_상태_필드_누락시_400_에러")
        void 잠금_상태_필드_누락시_400_에러() throws Exception {
        // given
        setupAdminAuthentication();
        String invalidJson = "{}";

        // when & then
        mockMvc.perform(patch("/api/users/{userId}/lock", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
        }
    }
}
