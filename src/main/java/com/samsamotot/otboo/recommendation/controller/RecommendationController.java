package com.samsamotot.otboo.recommendation.controller;

import static com.samsamotot.otboo.common.util.AuthUtil.getAuthenticatedUserId;

import com.samsamotot.otboo.recommendation.controller.api.RecommendationApi;
import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import com.samsamotot.otboo.recommendation.service.RecommendationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천(Recommendation) 관련 API 요청을 처리하는 REST 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController implements RecommendationApi {

    private final static String CONTROLLER = "[RecommendationController] ";
    private final RecommendationService recommendationService;

    @Override
    @GetMapping
    public ResponseEntity<RecommendationDto> getRecommendations(
        @RequestParam UUID weatherId
    ) {
        UUID userId = getAuthenticatedUserId();

        log.info(CONTROLLER + "추천 조회 요청: weatherId={}, userId={}", weatherId, userId);
        RecommendationDto recommendationDto = recommendationService.recommendClothes(userId, weatherId);
        log.info(CONTROLLER + "추천 조회 완료: {}", recommendationDto);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(recommendationDto);
    }
}
