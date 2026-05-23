package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.AdminMemoryResponse;
import com.vanilo.psych.agent.dto.ConversationMessageResponse;
import com.vanilo.psych.agent.service.ConversationMemoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/memories")
public class AdminMemoryController {
    private final ConversationMemoryService conversationMemoryService;

    public AdminMemoryController(ConversationMemoryService conversationMemoryService) {
        this.conversationMemoryService = conversationMemoryService;
    }

    @GetMapping
    public List<AdminMemoryResponse> listMemories(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
        return conversationMemoryService.listAdminMemories();
    }

    @GetMapping("/{username}")
    public AdminMemoryResponse getMemory(@PathVariable String username,
                                         Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
        return conversationMemoryService.getAdminMemory(username);
    }

    @GetMapping("/{username}/sessions/{sessionId}/messages")
    public List<ConversationMessageResponse> listMessages(@PathVariable String username,
                                                          @PathVariable Long sessionId,
                                                          Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
        return conversationMemoryService.listAdminMessages(username, sessionId);
    }
}
