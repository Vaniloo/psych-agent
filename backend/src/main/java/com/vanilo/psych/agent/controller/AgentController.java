package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.AgentChatRequest;
import com.vanilo.psych.agent.dto.AgentChatResponse;
import com.vanilo.psych.agent.service.AgentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
public class AgentController {
    private final AgentService agentService;
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }
    @PostMapping("/chat")
    public AgentChatResponse chat(@RequestBody AgentChatRequest agentChatRequest,
                                  Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
        return agentService.chat(agentChatRequest,authentication.getName());
    }
}
