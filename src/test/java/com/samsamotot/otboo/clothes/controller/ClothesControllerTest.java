package com.samsamotot.otboo.clothes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeDto;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeWithDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesSearchRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesUpdateRequest;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.exception.ClothesNotFoundException;
import com.samsamotot.otboo.clothes.service.ClothesService;
import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.user.entity.User;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(ClothesController.class)
@Import(SecurityTestConfig.class)
public class ClothesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClothesService clothesService;

    @Autowired
    private ObjectMapper objectMapper;

    User mockUser;
    CustomUserDetails mockPrincipal;

    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createValidUser();
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        mockPrincipal = new CustomUserDetails(mockUser);
    }

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
                UUID.randomUUID(),
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

            when(clothesService.create(any(UUID.class), any())).thenReturn(responseDto);

            MockMultipartFile jsonPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
            );

            // when & then
            mockMvc.perform(multipart("/api/clothes").file(jsonPart)
                .with(user(mockPrincipal)))
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
                UUID.randomUUID(),
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

            when(clothesService.create(any(UUID.class), any(), any())).thenReturn(responseDto);

            MockMultipartFile jsonPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
            );

            MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content".getBytes()
            );

            // when & then
            mockMvc.perform(multipart("/api/clothes")
                    .file(jsonPart)
                    .file(imagePart)
                    .with(user(mockPrincipal)))
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
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(invalidRequest)
            );

            // when & then
            mockMvc.perform(multipart("/api/clothes").file(jsonPart)
                    .with(user(mockPrincipal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionName").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("의상 수정 컨트롤러 테스트")
    class ClothesUpdateTest {
        @Test
        void 유효한_JSON을_보내면_200과_DTO가_반환된다() throws Exception {
            // given
            UUID clothesId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID defId = UUID.randomUUID();

            ClothesUpdateRequest request = new ClothesUpdateRequest(
                "업데이트된 티셔츠",
                ClothesType.TOP,
                List.of(new ClothesAttributeDto(defId, "여름"))
            );

            ClothesDto responseDto = new ClothesDto(
                clothesId,
                ownerId,
                "업데이트된 티셔츠",
                null,
                ClothesType.TOP,
                List.of(new ClothesAttributeWithDefDto(defId, "계절", List.of("봄", "여름", "가을", "겨울"), "여름"))
            );

            when(clothesService.update(eq(clothesId), any(ClothesUpdateRequest.class)))
                .thenReturn(responseDto);

            MockMultipartFile jsonPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
            );

            // when & then
            mockMvc.perform(multipart(HttpMethod.PATCH, "/api/clothes/{clothesId}", clothesId).file(jsonPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clothesId.toString()))
                .andExpect(jsonPath("$.name").value("업데이트된 티셔츠"))
                .andExpect(jsonPath("$.attributes[0].value").value("여름"));
        }

        @Test
        void 유효한_이미지와_JSON을_보내면_200과_DTO가_반환된다() throws Exception {
            // given
            UUID clothesId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID defId = UUID.randomUUID();

            ClothesUpdateRequest request = new ClothesUpdateRequest(
                "청자켓",
                ClothesType.OUTER,
                List.of(new ClothesAttributeDto(defId, "흑청"))
            );

            ClothesDto responseDto = new ClothesDto(
                clothesId,
                ownerId,
                "청자켓",
                "http://fake-s3-url/test.png",
                ClothesType.OUTER,
                List.of(new ClothesAttributeWithDefDto(defId, "색상", List.of("흑청","진청","연청"), "흑청"))
            );

            when(clothesService.update(eq(clothesId), any(ClothesUpdateRequest.class), any(MultipartFile.class)))
                .thenReturn(responseDto);

            MockMultipartFile jsonPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
            );

            MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content".getBytes()
            );

            // when & then
            mockMvc.perform(multipart(HttpMethod.PATCH, "/api/clothes/{clothesId}", clothesId)
                    .file(jsonPart)
                    .file(imagePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clothesId.toString()))
                .andExpect(jsonPath("$.name").value("청자켓"))
                .andExpect(jsonPath("$.imageUrl").value("http://fake-s3-url/test.png"))
                .andExpect(jsonPath("$.attributes[0].value").value("흑청"));
        }

        @Test
        void 잘못된_요청이라면_400코드와_에러메시지를_반환해야_한다() throws Exception {
            // given: 유효하지 않은 요청
            UUID clothesId = UUID.randomUUID();
            ClothesUpdateRequest invalidRequest = new ClothesUpdateRequest(
                "   ",
                null,
                Collections.emptyList()
            );

            MockMultipartFile jsonPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(invalidRequest)
            );

            // when & then
            mockMvc.perform(multipart(HttpMethod.PATCH, "/api/clothes/{clothesId}", clothesId)
                    .file(jsonPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionName").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("의상 삭제 컨트롤러 테스트")
    class ClothesDeleteTest {

        @Test
        void 유효한_ID로_삭제요청시_204를_반환한다() throws Exception {
            // given
            UUID clothesId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId))
                .andExpect(status().isNoContent());

            verify(clothesService).delete(clothesId);
        }

        @Test
        void 존재하지_않는_ID로_삭제요청시_404를_반환한다() throws Exception {
            // given
            UUID notExistId = UUID.randomUUID();
            doThrow(new ClothesNotFoundException()).when(clothesService).delete(notExistId);

            // when & then
            mockMvc.perform(delete("/api/clothes/{clothesId}", notExistId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value("CLOTHES_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));

            verify(clothesService).delete(notExistId);
        }
    }

    @Nested
    @DisplayName("의상 목록 조회 컨트롤러 테스트")
    class ClothesReadTest {

        @Test
        void 유효한_요청이면_200과_CursorResponse가_반환된다() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();
            UUID defId = UUID.randomUUID();

            UUID clothesId2 = UUID.randomUUID();
            UUID defId2 = UUID.randomUUID();

            ClothesDto clothesDto1 = new ClothesDto(
                clothesId,
                ownerId,
                "티셔츠",
                null,
                ClothesType.TOP,
                List.of(new ClothesAttributeWithDefDto(
                    defId,
                    "계절",
                    List.of("봄", "여름", "가을", "겨울"),
                    "여름"
                ))
            );

            ClothesDto clothesDto2 = new ClothesDto(
                clothesId2,
                ownerId,
                "바지",
                null,
                ClothesType.BOTTOM,
                List.of(new ClothesAttributeWithDefDto(
                    defId2,
                    "재질",
                    List.of("면", "울", "코듀로이", "실크"),
                    "코듀로이"
                ))
            );

            CursorResponse<ClothesDto> response = CursorResponse.<ClothesDto>builder()
                .data(List.of(clothesDto1, clothesDto2))
                .nextCursor("2025-09-26T04:47:51Z")
                .nextIdAfter(clothesId2)
                .hasNext(false)
                .totalCount(2L)
                .sortBy("createdAt")
                .sortDirection(SortDirection.DESCENDING)
                .build();


            when(clothesService.find(any(ClothesSearchRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/clothes")
                    .param("ownerId", ownerId.toString())
                    .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(clothesId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("티셔츠"))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"))
                .andExpect(jsonPath("$.totalCount").value(2));
        }

        @Test
        void typeEqual이_있다면_해당_값을_type_으로_가지는_정보만_조회되어야_한다() throws Exception {
            UUID ownerId = UUID.randomUUID();
            UUID clothesId1 = UUID.randomUUID();
            UUID clothesId2 = UUID.randomUUID();

            UUID defId1 = UUID.randomUUID();
            UUID defId2 = UUID.randomUUID();

            ClothesDto clothesDto1 = new ClothesDto(
                clothesId1,
                ownerId,
                "티셔츠",
                null,
                ClothesType.TOP,
                List.of(new ClothesAttributeWithDefDto(
                    defId1,
                    "계절",
                    List.of("봄", "여름", "가을", "겨울"),
                    "여름"
                ))
            );

            ClothesDto clothesDto2 = new ClothesDto(
                clothesId2,
                ownerId,
                "스웨터",
                null,
                ClothesType.TOP,
                List.of(new ClothesAttributeWithDefDto(
                    defId2,
                    "재질",
                    List.of("면", "울", "코듀로이", "실크"),
                    "울"
                ))
            );

            CursorResponse<ClothesDto> response = CursorResponse.<ClothesDto>builder()
                .data(List.of(clothesDto1, clothesDto2))
                .nextCursor("2025-09-26T04:47:51Z")
                .nextIdAfter(clothesId2)
                .hasNext(false)
                .totalCount(2L)
                .sortBy("createdAt")
                .sortDirection(SortDirection.DESCENDING)
                .build();

            when(clothesService.find(any(ClothesSearchRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/clothes")
                    .param("ownerId", ownerId.toString())
                    .param("typeEqual", "TOP")
                    .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(clothesId1.toString()))
                .andExpect(jsonPath("$.data[0].type").value("TOP"))
                .andExpect(jsonPath("$.data[1].type").value("TOP"))
                .andExpect(jsonPath("$.totalCount").value(2));

        }

        @Test
        void 잘못된_요청이라면_400코드와_에러메시지를_반환한다() throws Exception {
            // given: 유효하지 않은 요청
            UUID ownerId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/clothes")
                    .param("ownerId", ownerId.toString())
                    .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionName").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
        }
    }
}
