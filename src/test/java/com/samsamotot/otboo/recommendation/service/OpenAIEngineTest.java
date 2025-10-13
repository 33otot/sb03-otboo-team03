package com.samsamotot.otboo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsamotot.otboo.recommendation.prompt.PromptBuilder;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAIEngine 단위 테스트")
public class OpenAIEngineTest {

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private OpenAiChatModel openAiChatModel;

    @InjectMocks
    private OpenAIEngine openAIEngine;

    @Test
    void LLM_정상_응답시_후처리된_문구를_반환한다() {

        // given
        String userPrompt = "user prompt";
        String llmRaw = "추천 결과입니다.";
        String processed = "후처리된 추천 결과입니다.";

        // Mock LLM 응답 구성
        AssistantMessage assistantMessage = new AssistantMessage(llmRaw);
        Generation generation = new Generation(assistantMessage);
        ChatResponse response = new ChatResponse(List.of(generation));

        when(promptBuilder.buildOneLiner(any(), anyBoolean(), any(), any(), any(), any()))
            .thenReturn(userPrompt);
        when(promptBuilder.postProcessOneLiner(llmRaw)).thenReturn(processed);
        when(openAiChatModel.call(any(Prompt.class))).thenReturn(response);

        // when
        String reason = openAIEngine.generateRecommendationReason(
            20.0, false, Month.APRIL, 20.0, 2.5, List.of()
        );

        // then
        assertThat(reason).isEqualTo(processed);
        verify(promptBuilder).postProcessOneLiner(llmRaw);
    }

    @Test
    void LLM_예외시_fallback_문구를_반환한다() {

        // given
        String userPrompt = "user prompt";

        when(promptBuilder.buildOneLiner(any(), anyBoolean(), any(), any(), any(), any()))
            .thenReturn(userPrompt);
        when(openAiChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("LLM 오류"));
        when(promptBuilder.fallbackOneLiner()).thenReturn("기본 추천 문구");

        // when
        String reason = openAIEngine.generateRecommendationReason(
            20.0, false, Month.APRIL, 20.0, 2.5, List.of()
        );

        // then
        assertThat(reason).isEqualTo("기본 추천 문구");
        verify(promptBuilder).fallbackOneLiner();
    }
}
