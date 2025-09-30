package com.samsamotot.otboo.directmessage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.service.DirectMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.controller
 * FileName     : DirectMessageControllerTest
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@WebMvcTest(DirectMessageController.class)
@DisplayName("Direct Message 컨트롤러 슬라이스 테스트")
class DirectMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DirectMessageService directMessageService;

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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }
}