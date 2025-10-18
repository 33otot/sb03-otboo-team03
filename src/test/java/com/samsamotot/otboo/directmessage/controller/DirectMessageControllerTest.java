package com.samsamotot.otboo.directmessage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.fixture.ProfileFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomListResponse;
import com.samsamotot.otboo.directmessage.dto.DmTopicKey;
import com.samsamotot.otboo.directmessage.dto.SendDmRequest;
import com.samsamotot.otboo.directmessage.service.DirectMessageService;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.controller
 * FileName     : DirectMessageControllerTest
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@WebMvcTest(DirectMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityTestConfig.class)
@DisplayName("Direct Message 컨트롤러 슬라이스 테스트")
class DirectMessageControllerTest {

    @Autowired
    private DirectMessageController controller;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DirectMessageService directMessageService;

    @MockitoBean
    SimpMessagingTemplate simpMessagingTemplate;

    @Test
    void dm을_목록을_정상적으로_가져온다() throws Exception {
        // given
        var response = DirectMessageListResponse.builder()
            .data(List.of())
            .hasNext(false)
            .totalCount(1L)
            .sortBy("createdAt")
            .sortDirection("DESCENDING")
            .build();
        given(directMessageService.getMessages(any())).willReturn(response);

        // when n then
        mockMvc.perform(get("/api/direct-messages")
                .param("userId", UUID.randomUUID().toString())
                .param("limit", "10"))
            .andExpect(status().isOk());
    }

    @Test
    void dm없으면_안_가져온다() throws Exception {
        // given
        var response = DirectMessageListResponse.builder()
            .data(List.of())
            .hasNext(false)
            .totalCount(0L)
            .sortBy("createdAt")
            .sortDirection("DESCENDING")
            .build();
        given(directMessageService.getMessages(any())).willReturn(response);

        // when n then
        mockMvc.perform(get("/api/direct-messages")
                .param("userId", UUID.randomUUID().toString())
                .param("limit", "10"))
            .andDo(print())
            .andExpect(status().isOk());
    }

    /*
        메세지 전송 기능
     */
    @Test
    void 메세지_정상_전송() throws Exception {
        // given
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var principal = new Principal() {
            @Override public String getName() { return me.toString(); }
        };

        SendDmRequest request = new SendDmRequest(UUID.randomUUID(), other, "hello ws");

        DirectMessageDto dto = org.mockito.Mockito.mock(DirectMessageDto.class);
        given(directMessageService.sendMessage(any(), any())).willReturn(dto);

        // when
        DirectMessageDto resp = controller.send(request, principal);

        // then
        assertThat(resp).isSameAs(dto);

        then(directMessageService)
            .should().sendMessage(eq(me), eq(request));

        // 브로드캐스트 목적지 검증
        String expectedDestination = DmTopicKey.destination(me, other);
        then(simpMessagingTemplate)
            .should().convertAndSend(eq(expectedDestination),
                eq(dto));
    }

    @Test
    void 인증_없는_웹소캣_null() throws Exception {
        // given
        UUID other = UUID.randomUUID();
        var req = new SendDmRequest(UUID.randomUUID(), other, "no auth");

        // when
        DirectMessageDto resp = controller.send(req, null);

        // then
        assertThat(resp).isNull();
        then(directMessageService).shouldHaveNoInteractions();
        then(simpMessagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    void 정상적으로_대화방_목록을_가져온다() throws Exception {
        // given
        DirectMessageRoomDto mockRoomDto = DirectMessageRoomDto.builder()
            .partner(AuthorDto.builder()
                .userId(UUID.randomUUID())
                .name("testUser")
                .build())
            .lastMessage("Hello")
            .lastMessageSentAt(Instant.now())
            .build();

        DirectMessageRoomListResponse mockResponse = DirectMessageRoomListResponse.builder()
            .rooms(List.of(mockRoomDto))
            .build();
        given(directMessageService.getConversationList()).willReturn(mockResponse);

        User mockUser = UserFixture.createValidUser();
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        Profile mockProfile = ProfileFixture.createProfile(mockUser);
        ReflectionTestUtils.setField(mockUser, "profile", mockProfile);
        CustomUserDetails mockPrincipal = new CustomUserDetails(mockUser);

        // when & then
        mockMvc.perform(get("/api/direct-messages/rooms")
                .with(user(mockPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rooms.length()").value(1));

        then(directMessageService).should().getConversationList();
    }
}