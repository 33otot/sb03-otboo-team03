package com.samsamotot.otboo.recommendation.llm;

public interface OpenAiClient {

    String generateOneLiner(String systemPrompt, String userPrompt);
}
