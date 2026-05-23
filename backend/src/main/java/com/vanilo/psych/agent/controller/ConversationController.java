package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.ConversationMessageResponse;
import com.vanilo.psych.agent.dto.ConversationSessionResponse;
import com.vanilo.psych.agent.service.ConversationMemoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
public class ConversationController {
    private final ConversationMemoryService conversationMemoryService;

    public ConversationController(ConversationMemoryService conversationMemoryService) {
        this.conversationMemoryService = conversationMemoryService;
    }

    @GetMapping
    public List<ConversationSessionResponse> listSessions(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
        return conversationMemoryService.listSessions(authentication.getName());
    }

    @GetMapping("/{sessionId}/messages")
    public List<ConversationMessageResponse> listMessages(@PathVariable Long sessionId,
                                                          Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
        return conversationMemoryService.listMessages(authentication.getName(), sessionId);
    }
}
