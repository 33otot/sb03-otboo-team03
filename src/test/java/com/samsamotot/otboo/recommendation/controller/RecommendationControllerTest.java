package com.samsamotot.otboo.recommendation.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import com.samsamotot.otboo.recommendation.service.RecommendationService;
import com.samsamotot.otboo.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecommendationController.class)
@Import(SecurityTestConfig.class)
@DisplayName("Recommendation 컨트롤러 슬라이스 테스트")
public class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    User mockUser;
    CustomUserDetails mockPrincipal;

    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createValidUser();
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        mockPrincipal = new CustomUserDetails(mockUser);
    }

    @Test
    void 유효한_조회_요청이면_추천_결과와_200이_반환되어야_한다() throws Exception {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = UUID.randomUUID();

        RecommendationDto mockDto = RecommendationDto.builder()
            .userId(userId)
            .weatherId(weatherId)
            .clothes(List.of())
            .build();
        given(recommendationService.recommendClothes(userId, weatherId)).willReturn(mockDto);

        // when & then
        mockMvc.perform(get("/api/recommendations")
                .param("weatherId", weatherId.toString())
                .with(user(mockPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clothes").isArray());
    }

    @Test
    void 존재하지_않는_날씨면_404가_반환되어야_한다() throws Exception {

        // given
        UUID userId = mockUser.getId();
        UUID weatherId = UUID.randomUUID();

        given(recommendationService.recommendClothes(userId, weatherId))
            .willThrow(new OtbooException(ErrorCode.WEATHER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/recommendations")
                .param("weatherId", weatherId.toString())
                .with(user(mockPrincipal)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.exceptionName").value(ErrorCode.WEATHER_NOT_FOUND.toString()))
            .andExpect(jsonPath("$.message").value(ErrorCode.WEATHER_NOT_FOUND.getMessage()))
            .andExpect(jsonPath("$.code").value(ErrorCode.WEATHER_NOT_FOUND.getCode()));
    }

    @Test
    void weatherId가_없으면_400이_반환되어야_한다() throws Exception {

        // when & then
        mockMvc.perform(get("/api/recommendations")
                .with(user(mockPrincipal)))
            .andExpect(status().isBadRequest());
    }
}
