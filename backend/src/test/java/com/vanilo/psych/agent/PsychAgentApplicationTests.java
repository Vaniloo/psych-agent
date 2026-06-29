package com.vanilo.psych.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test requires MySQL, Redis, Chroma and Ollama")
class PsychAgentApplicationTests {

    @Test
    void contextLoads() {
    }

}
