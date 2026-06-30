package com.vanilo.psych.agent.config;

import com.vanilo.psych.agent.service.KnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSeedRunnerTests {
    @Test
    void embeddingFailureDoesNotAbortApplicationStartup() {
        KnowledgeService knowledgeService = mock(KnowledgeService.class);
        when(knowledgeService.listAllDocuments()).thenThrow(new RuntimeException("embedding unavailable"));
        KnowledgeSeedRunner runner = new KnowledgeSeedRunner(knowledgeService, true);

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments()));
    }
}
