package com.samsamotot.otboo.recommendation.llm;

import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OpenAiClientImpl implements OpenAiClient{

    private final WebClient client;

    public OpenAiClientImpl(@Qualifier("openAiWebClient") WebClient client) {
        this.client = client;
    }

    @Override
    public String generateOneLiner(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
            "model", "gpt-4o-mini",
            "input", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
            ),
            "max_output_tokens", 64,
            "temperature", 0.3
        );

        return client.post()
            .uri("/v1/responses")
            .bodyValue(body)
            .exchangeToMono(res -> {
                String rid = res.headers().asHttpHeaders().getFirst("x-request-id");
                if (res.statusCode().is2xxSuccessful()) {
                    return res.bodyToMono(Map.class).map(OpenAiClientImpl::extractTextFromResponses);
                } else {
                    return res.bodyToMono(String.class).defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new IllegalStateException(
                            "OpenAI " + res.statusCode() + " rid=" + rid + " body=" + abbreviate(err, 300))));
                }
            })
            .block();
    }

    @SuppressWarnings("unchecked")
    private static String extractTextFromResponses(Map<String, Object> resp) {
        // Responses API: resp.output[*].content[*] (type=output_text)
        Object outObj = resp.get("output");
        if (outObj instanceof List<?> output) {
            StringBuilder sb = new StringBuilder();
            for (Object itemObj : output) {
                if (!(itemObj instanceof Map)) continue;
                Map<String, Object> item = (Map<String, Object>) itemObj;

                Object contentObj = item.get("content");
                if (!(contentObj instanceof List<?> contentList)) continue;

                for (Object blockObj : contentList) {
                    if (!(blockObj instanceof Map)) continue;
                    Map<String, Object> block = (Map<String, Object>) blockObj;

                    // 출력 타입이 명시되면 output_text만 취급(기타 tool_call 등 무시)
                    Object type = block.get("type");
                    if (type instanceof String && !"output_text".equals(type)) continue;

                    Object textObj = block.get("text");
                    if (textObj instanceof String s) {
                        sb.append(s);
                    } else if (textObj instanceof Map) {
                        Object val = ((Map<String, Object>) textObj).get("value");
                        if (val instanceof String v) sb.append(v);
                    }
                }
            }
            if (sb.length() > 0) return sb.toString().trim();
        }

        // 폴백: Chat Completions 포맷 (choices[0].message.content)
        Object choicesObj = resp.get("choices");
        if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map) {
                Map<String, Object> firstMap = (Map<String, Object>) first;
                Object msgObj = firstMap.get("message");
                if (msgObj instanceof Map) {
                    Object content = ((Map<String, Object>) msgObj).get("content");
                    if (content instanceof String s) return s.trim();
                }
            }
        }

        // 혹시 모를 단순 문자열 컨텐트 폴백
        Object content = resp.get("content");
        if (content instanceof String s) return s.trim();

        return "";
    }
}
