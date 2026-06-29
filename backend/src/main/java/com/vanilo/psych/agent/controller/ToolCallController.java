package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.ToolCallRequest;
import com.vanilo.psych.agent.dto.ToolExecutionResponse;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.service.ToolCallService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tools")
public class ToolCallController {
    private final ToolCallService toolCallService;

    public ToolCallController(ToolCallService toolCallService) {
        this.toolCallService = toolCallService;
    }

    @PostMapping("/call")
    public ToolExecutionResponse callTool(@RequestBody ToolCallRequest request, Authentication authentication) {
        injectCurrentUser(request, authentication);
        return toolCallService.call(request);
    }

    @GetMapping
    public List<ToolInfoResponse> listTools() {
        return toolCallService.listTools();
    }

    @GetMapping("/{toolName}")
    public ToolInfoResponse getTool(@PathVariable String toolName) {
        return toolCallService.getTool(toolName);
    }

    @PostMapping("/{toolName}")
    public ToolExecutionResponse callToolByName(@PathVariable String toolName,
                                                @RequestBody(required = false) Map<String, Object> arguments,
                                                Authentication authentication) {
        ToolCallRequest request = new ToolCallRequest(toolName, arguments);
        injectCurrentUser(request, authentication);
        return toolCallService.call(request);
    }

    private void injectCurrentUser(ToolCallRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
        if (request.getArguments() == null) {
            request.setArguments(new HashMap<>());
        }
        if (requiresCurrentUser(request.getTool())) {
            request.getArguments().put("username", authentication.getName());
        }
    }

    private boolean requiresCurrentUser(String toolName) {
        return "get_dashboard".equals(toolName)
                || "get_user_memory".equals(toolName)
                || "recommend_strategy".equals(toolName);
    }
}
