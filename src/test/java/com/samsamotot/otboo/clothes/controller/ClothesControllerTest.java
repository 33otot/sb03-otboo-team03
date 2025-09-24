package com.samsamotot.otboo.clothes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeDto;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeWithDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.service.ClothesService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClothesController.class)
public class ClothesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClothesService clothesService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("의상 등록 컨트롤러 테스트")
    class ClothesCreateTest {

        @Test
        void 정상적인_요청이라면_등록에_성공하고_201코드와_DTO를_반환해야_한다() throws Exception {

            // given
            UUID ownerId = UUID.randomUUID();
            UUID defId1 = UUID.randomUUID();
            UUID defId2 = UUID.randomUUID();
            // given
            ClothesCreateRequest request = new ClothesCreateRequest(
                ownerId,
                "챠르르 셔츠",
                ClothesType.TOP,
                List.of(new ClothesAttributeDto(defId1, "옵션1"), new ClothesAttributeDto(defId2, "옵션2"))
            );

            ClothesDto responseDto = new ClothesDto(
              UUID.randomUUID(),
              ownerId,
              "챠르르 셔츠",
                null,
                ClothesType.TOP,
                List.of(new ClothesAttributeWithDefDto(defId1, "정의1", List.of("옵션1", "옵션12", "옵션13"), "옵션1"), new ClothesAttributeWithDefDto(defId2, "정의2", List.of("옵션2", "옵션22"), "옵션2"))
            );

            when(clothesService.create(any())).thenReturn(responseDto);

            MockMultipartFile jsonPart = new MockMultipartFile(
                "clothesCreateRequest",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
            );

            // when & then
            mockMvc.perform(multipart("/api/clothes").file(jsonPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("챠르르 셔츠"))
                .andExpect(jsonPath("$.attributes[0].value").value("옵션1"));
        }

        @Test
        void 유효한_이미지와_JSON을_함께_보내면_201과_DTO가_반환된다() throws Exception {

            // given
            UUID ownerId = UUID.randomUUID();
            UUID defId = UUID.randomUUID();

            ClothesCreateRequest request = new ClothesCreateRequest(
                ownerId,
                "청자켓",
                ClothesType.OUTER,
                List.of(new ClothesAttributeDto(defId, "흑청"))
            );

            ClothesDto responseDto = new ClothesDto(
                UUID.randomUUID(),
                ownerId,
                "청자켓",
                "http://fake-s3-url/test.png",   // 가짜 URL
                ClothesType.OUTER,
                List.of(new ClothesAttributeWithDefDto(defId, "색상", List.of("흑청","진청","연청"), "흑청"))
            );

            when(clothesService.create(any(), any())).thenReturn(responseDto);

            MockMultipartFile jsonPart = new MockMultipartFile(
                "clothesCreateRequest",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
            );

            MockMultipartFile imagePart = new MockMultipartFile(
                "clothesImage",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content".getBytes()
            );

            // when & then
            mockMvc.perform(multipart("/api/clothes")
                    .file(jsonPart)
                    .file(imagePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("청자켓"))
                .andExpect(jsonPath("$.imageUrl").value("http://fake-s3-url/test.png"))
                .andExpect(jsonPath("$.attributes[0].value").value("흑청"));
        }

        @Test
        void 잘못된_요청이라면_400코드와_에러메시지를_반환해야_한다() throws Exception {

            // given: 유효하지 않은 요청
            ClothesCreateRequest invalidRequest = new ClothesCreateRequest(
                null,  // ownerId 누락
                "   ",          // name 공백
                null,           // type 누락
                Collections.emptyList()
            );

            MockMultipartFile jsonPart = new MockMultipartFile(
                "clothesCreateRequest",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(invalidRequest)
            );

            // when & then
            mockMvc.perform(multipart("/api/clothes").file(jsonPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionName").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
        }
    }
}
