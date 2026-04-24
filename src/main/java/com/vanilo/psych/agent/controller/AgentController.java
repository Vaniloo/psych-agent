package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.AgentChatRequest;
import com.vanilo.psych.agent.dto.AgentChatResponse;
import com.vanilo.psych.agent.service.AgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
public class AgentController {
    private final AgentService agentService;
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }
    @PostMapping("/chat")
    public AgentChatResponse chat(@RequestBody AgentChatRequest agentChatRequest){
        return agentService.chat(agentChatRequest);
    }
}
