package com.samsamotot.otboo.recommendation.service;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.recommendation.llm.OpenAiClientImpl;
import com.samsamotot.otboo.recommendation.prompt.PromptBuilder;
import java.time.Month;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIEngine {

    private final PromptBuilder promptBuilder;
    private final OpenAiClientImpl openAiClient;

    private static final String ENGINE = "[OpenAIEngine] ";

    /**
     * 추천 결과에 대한 한 문장 설명을 생성합니다.
     * 예외 발생시, 대체 문구를 반환합니다. (fallback)
     */
    public String generateRecommendationReason(
        Double temperature,
        boolean isRainingOrSnowing,
        Month currentMonth,
        Double feelsLike,
        Double sensitivity,
        List<OotdDto> recommendedItems
    ) {
        try {
            log.debug(ENGINE + "LLM 프롬프트 생성 시작: 온도={}, 비/눈={}, 월={}, 체감온도={}, 민감도={}, 아이템={}",
                temperature, isRainingOrSnowing, currentMonth, feelsLike, sensitivity, recommendedItems);

            String userPrompt = promptBuilder.buildOneLiner(temperature, isRainingOrSnowing, currentMonth, feelsLike, sensitivity, recommendedItems);
            log.debug(ENGINE + "LLM 유저 프롬프트 생성 완료: {}", userPrompt);

            String raw = openAiClient.generateOneLiner(PromptBuilder.SYSTEM_PROMPT, userPrompt);
            log.debug(ENGINE + "LLM 응답 수신: {}", raw);

            String result = promptBuilder.postProcessOneLiner(raw);
            log.debug(ENGINE + "LLM 응답 후처리 완료: {}", result);

            return result;
        } catch (Exception e) {
            log.warn(ENGINE + "LLM 응답 처리 중 예외 발생: {}", e.getMessage(), e);
            return promptBuilder.fallbackOneLiner();
        }
    }
}
