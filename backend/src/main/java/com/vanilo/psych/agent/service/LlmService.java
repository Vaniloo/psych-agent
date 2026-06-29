package com.vanilo.psych.agent.service;

public interface LlmService {
    String complete(String systemPrompt, String userPrompt);

    String provider();
}
