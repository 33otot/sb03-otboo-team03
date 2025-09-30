package com.samsamotot.otboo.clothes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import com.samsamotot.otboo.clothes.service.ClothesAttributeDefService;
import com.samsamotot.otboo.config.TestConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public class ClothesAttributeDefControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClothesAttributeDefService defService;

    @Test
    @WithMockUser(roles = "USER") // 일반 유저
    void USER_권한은_create_API_호출시_403을_반환한다() throws Exception {
        ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
            "속성명", List.of("옵션1", "옵션2")
        );

        mockMvc.perform(post("/api/clothes/attribute-defs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden()); // 403
    }

    @Test
    @WithMockUser(roles = "ADMIN") // 관리자 유저
    void ADMIN_권한은_create_API_호출시_201을_반환한다() throws Exception {
        UUID defId = UUID.randomUUID();

        ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
            "속성명", List.of("옵션1", "옵션2")
        );

        given(defService.create(any()))
            .willReturn(new ClothesAttributeDefDto(defId, "속성명", List.of("옵션1", "옵션2"), Instant.now()));

        mockMvc.perform(post("/api/clothes/attribute-defs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated()); // 201
    }

    @Test
    @WithMockUser(roles = "USER") // 일반 유저
    void USER_권한은_update_API_호출시_403을_반환한다() throws Exception {
        UUID defId = UUID.randomUUID();

        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            "속성명", List.of("옵션1", "옵션2")
        );

        mockMvc.perform(patch("/api/clothes/attribute-defs/{definitionId}", defId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden()); // 403
    }

    @Test
    @WithMockUser(roles = "ADMIN") // 관리자 유저
    void ADMIN_권한은_update_API_호출시_200을_반환한다() throws Exception {
        UUID defId = UUID.randomUUID();

        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest (
            "속성명", List.of("옵션1", "옵션2")
        );

        given(defService.update(eq(defId), any()))
            .willReturn(new ClothesAttributeDefDto(defId, "속성명", List.of("옵션1", "옵션2"), Instant.now()));

        mockMvc.perform(patch("/api/clothes/attribute-defs/{definitionId}", defId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk()); // 200
    }
}