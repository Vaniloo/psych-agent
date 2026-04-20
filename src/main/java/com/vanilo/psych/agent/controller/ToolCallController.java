package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.ToolCallRequest;
import com.vanilo.psych.agent.service.ToolCallService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tools")
public class ToolCallController {
    private final ToolCallService toolCallService;
    public ToolCallController(ToolCallService toolCallService) {
        this.toolCallService = toolCallService;
    }
    @PostMapping("/call")
    public Object callTool(@RequestBody ToolCallRequest toolCallRequest){
        return toolCallService.call(toolCallRequest);
    }
}
