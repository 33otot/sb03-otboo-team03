package com.samsamotot.otboo.follow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.service.FollowService;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.print.attribute.standard.Media;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PackageName  : com.samsamotot.otboo.follow.controller
 * FileName     : FollowControllerTest
 * Author       : dounguk
 * Date         : 2025. 9. 13.
 */
@WebMvcTest(FollowController.class)
@DisplayName("팔로우 컨트롤러 슬라이스 테스트")
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FollowService followService;

    /**
     * private User(
     * String email,
     * String name,
     * String password,
     * Role role,
     * Boolean locked,
     * Instant temporaryPasswordExpiresAt
     * ) {
     */
    static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    static final String EMAIL = "test@test.com";
    static final String NAME = "name";
    static final String PASSWORD = "password";
    static final Role ROLE = Role.USER;
    static final boolean LOCKED = false;
    static final Instant TEMPORARY_PASSWORD_EXPIRESAT = Instant.now().plusSeconds(60*10);

    @Test
    void 바디를_정상입력_받을경우_팔로우를_성공한다() throws Exception {
        // given
        User follower = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);
        User followee = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);

        FollowCreateRequest validRequest = new FollowCreateRequest(follower.getId(), followee.getId());
        FollowDto response = new FollowDto(UUID.randomUUID(), followee, follower);

        // when
        when(followService.follow(any(FollowCreateRequest.class)))
            .thenReturn(response);

        // when n then
        mockMvc.perform(post("/api/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.followee").isMap())
            .andExpect(jsonPath("$.followee.id").value(followee.getId()))
            .andExpect(jsonPath("$.follower").isMap())
            .andExpect(jsonPath("$.follower.id").value(follower.getId()));
    }
}