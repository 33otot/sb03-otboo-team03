package com.samsamotot.otboo.clothes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.service.ClothesAttributeDefService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClothesAttributeDefController.class)
class ClothesAttributeDefControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClothesAttributeDefService defService;

    @Nested
    @DisplayName("의상 속성 정의 생성 컨트롤러 테스트")
    class ClothesAttributeDefCreateTest {

        @Test
        void 정상적인_요청이라면_등록에_성공하고_201코드와_DTO를_반환해야_한다() throws Exception {

            // given
            ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
                "테스트 속성 명",
                List.of("옵션1", "옵션2", "옵션3")
            );

            ClothesAttributeDefDto responseDto = new ClothesAttributeDefDto(
                UUID.randomUUID(),
                "테스트 속성 명",
                List.of("옵션1", "옵션2", "옵션3"),
                Instant.now()
            );

            when(defService.create(any())).thenReturn(responseDto);

            // when & then
            mockMvc.perform(post("/api/clothes/attribute-defs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("테스트 속성 명"))
                .andExpect(jsonPath("$.selectableValues[0]").value("옵션1"));
        }

        @Test
        void 등록에_실패하면_400코드를_반환해야_한다() throws Exception {
            // given
            ClothesAttributeDefCreateRequest invalidRequest  = new ClothesAttributeDefCreateRequest(
                null,
                List.of("옵션1", "옵션2", "옵션3")
            );

            // when & then
            mockMvc.perform(post("/api/clothes/attribute-defs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
        }
    }
}