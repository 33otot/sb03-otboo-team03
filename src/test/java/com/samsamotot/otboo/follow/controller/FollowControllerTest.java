package com.samsamotot.otboo.follow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.user.UserSummaryDto;
import com.samsamotot.otboo.follow.service.FollowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Test
    void 바디를_정상입력_받을경우_팔로우를_성공한다() throws Exception {
        // given
        UserSummaryDto followee = new UserSummaryDto(UUID.randomUUID(), "팔로위", "profileImageUrl");
        UserSummaryDto follower = new UserSummaryDto(UUID.randomUUID(), "팔로워", "profileImageUrl");

        FollowCreateRequest validRequest = new FollowCreateRequest(follower.userId(), followee.userId());
        FollowDto response = new FollowDto(UUID.randomUUID(), followee, follower);

        given(followService.follow(refEq(validRequest))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.followee").isMap())
            .andExpect(jsonPath("$.followee.userId").value(followee.userId().toString()))
            .andExpect(jsonPath("$.followee.username").value(followee.username()))
            .andExpect(jsonPath("$.followee.profileImageUrl").value(followee.profileImageUrl()))
            .andExpect(jsonPath("$.follower").isMap())
            .andExpect(jsonPath("$.follower.userId").value(follower.userId().toString()))
            .andExpect(jsonPath("$.follower.username").value(follower.username()))
            .andExpect(jsonPath("$.follower.profileImageUrl").value(follower.profileImageUrl()));

        then(followService).should(times(1)).follow(refEq(validRequest));
        then(followService).shouldHaveNoMoreInteractions();
    }

//        UUID followeeId, // 팔로우 대상 사용자 ID - given
//        Long followerCount, // 팔로워 수 - follow
//        Long followingCount, // 팔로잉 수 - follow
//        boolean followedByMe, // 내가 팔로우 대상 사용자를 팔로우 하고 있는지 여부 - follow
//        UUID followedByMeId, // 내가 팔로우 대상 사용자를 팔로우하고 있는 팔로우 ID - follow
//        boolean followingMe // 대상 사용자가 나를 팔로우하고 있는지 여부 - follow

    // findFollowerByFolloweeId
    // findFolloweeByFollowerId
    // existByFollowerAndFollowee 정방향
    // 위에서 true 면 아이디 가져옴
    // existByFollowerAndFollowee 역방향 조회

    @Test
    void 유저id를_정상입력_받을경우_요약된_팔로우_리스트를_응답한다() throws Exception {
        // given

        // when

        // then

    }

    @Test
    void 정상적으로_팔로워_수를_가져온다() throws Exception {
        // given

        // when

        // then

    }

    @Test
    void 정상적으로_팔로우_수를_가져온다() throws Exception {
        // given

        // when

        // then

    }

    @Test
    void 사용자_기준_팔로우_여부_확인() throws Exception {
        // given

        // when

        // then

    }
}