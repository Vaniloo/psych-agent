package com.vanilo.psych.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DefaultLlmService implements LlmService {
    private final ChatClient chatClient;
    private final String provider;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile Instant openUntil = Instant.EPOCH;

    public DefaultLlmService(ChatClient.Builder chatClientBuilder,
                             @Value("${psych.ai.provider:ollama}") String provider) {
        this.chatClient = chatClientBuilder.build();
        this.provider = provider;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (Instant.now().isBefore(openUntil)) {
            throw new RuntimeException("AI 服务暂时不可用，请稍后重试");
        }
        RuntimeException lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String content = chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
                consecutiveFailures.set(0);
                return content;
            } catch (RuntimeException e) {
                lastError = e;
            }
        }
        if (consecutiveFailures.incrementAndGet() >= 3) {
            openUntil = Instant.now().plus(Duration.ofSeconds(30));
            consecutiveFailures.set(0);
        }
        throw new RuntimeException("AI 服务调用失败", lastError);
    }

    @Override
    public String provider() {
        return provider;
    }
}
