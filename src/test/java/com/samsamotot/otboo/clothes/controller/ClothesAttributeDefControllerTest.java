package com.samsamotot.otboo.clothes.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefUpdateRequest;
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

    @Nested
    @DisplayName("의상 속성 정의 수정 컨트롤러 테스트")
    class ClothesAttributeDefUpdateTest {
        @Test
        void 수정에_성공하면_200코드를_반환해야_한다() throws Exception {

            // given
            UUID defId = UUID.randomUUID();
            ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
                "테스트 속성 명",
                List.of("옵션1", "옵션2", "옵션3")
            );

            ClothesAttributeDefDto responseDto = ClothesAttributeDefDto.builder()
                .id(defId)
                .name("테스트 속성 명")
                .selectableValues(List.of("옵션1", "옵션2", "옵션3"))
                .createdAt(Instant.now())
                .build();

            when(defService.update(eq(defId), any(ClothesAttributeDefUpdateRequest.class))).thenReturn(responseDto);

            // when & then
            mockMvc.perform(patch("/api/clothes/attribute-defs/{definitionId}", defId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("테스트 속성 명"))
                .andExpect(jsonPath("$.selectableValues", hasSize(3)))
                .andExpect(jsonPath("$.selectableValues[0]").value("옵션1"));
        }
    }

    @Nested
    @DisplayName("의상 속성 정의 삭제 컨트롤러 테스트")
    class ClothesAttributeDefDeleteTest {
        @Test
        void 의상_속성_정의_삭제_성공() throws Exception {

            // given
            UUID defId = UUID.randomUUID();

            mockMvc.perform(delete("/api/clothes/attribute-defs/{definitionId}", defId))
                .andExpect(status().isNoContent());

            verify(defService).delete(defId);
        }
    }

    @Nested
    @DisplayName("의상 속성 정의 목록 조회 컨트롤러 테스트")
    class ClothesAttributeDefFindTest {
        @Test
        void 조회에_성공하면_200코드와_dto리스트를_반환한다() throws Exception {

            ClothesAttributeDefDto responseDto1 = ClothesAttributeDefDto.builder()
                .id(UUID.randomUUID())
                .name("가나다 테스트 속성 명")
                .selectableValues(List.of("옵션1", "옵션2", "옵션3"))
                .createdAt(Instant.now())
                .build();

            ClothesAttributeDefDto responseDto2 = ClothesAttributeDefDto.builder()
                .id(UUID.randomUUID())
                .name("라마바 테스트 속성 명")
                .selectableValues(List.of("옵션12", "옵션22", "옵션32"))
                .createdAt(Instant.now())
                .build();

            List<ClothesAttributeDefDto> responseList = List.of(responseDto1, responseDto2);

            when(defService.findAll("name", "ASCENDING", null)).thenReturn(responseList);

            // when & then
            mockMvc.perform(get("/api/clothes/attribute-defs")
                .param("sortBy", "name")
                .param("sortDirection", "ASCENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("가나다 테스트 속성 명"))
                .andExpect(jsonPath("$[0].selectableValues[0]").value("옵션1"))
                .andExpect(jsonPath("$[1].name").value("라마바 테스트 속성 명"));
        }

        @Test
        void 키워드가_있다면_포함하는_결과가_반환된다() throws Exception {

            ClothesAttributeDefDto responseDto1 = ClothesAttributeDefDto.builder()
                .id(UUID.randomUUID())
                .name("가나다 테스트 속성 명")
                .selectableValues(List.of("옵션1", "옵션2", "옵션3"))
                .createdAt(Instant.now())
                .build();

            ClothesAttributeDefDto responseDto2 = ClothesAttributeDefDto.builder()
                .id(UUID.randomUUID())
                .name("라마바 테스트 속성 명")
                .selectableValues(List.of("옵션12", "옵션22", "옵션32"))
                .createdAt(Instant.now())
                .build();

            List<ClothesAttributeDefDto> responseList = List.of(responseDto1);

            when(defService.findAll("name", "ASCENDING", "가")).thenReturn(responseList);

            // when & then
            mockMvc.perform(get("/api/clothes/attribute-defs")
                    .param("sortBy", "name")
                    .param("sortDirection", "ASCENDING")
                    .param("keywordLike", "가"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("가나다 테스트 속성 명"))
                .andExpect(jsonPath("$[0].selectableValues[0]").value("옵션1"));
        }
    }
}