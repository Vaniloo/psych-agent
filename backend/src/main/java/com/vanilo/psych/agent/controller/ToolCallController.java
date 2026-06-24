package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.ToolCallRequest;
import com.vanilo.psych.agent.dto.ToolExecutionResponse;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.service.ToolCallService;
import org.springframework.web.bind.annotation.*;

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
    public ToolExecutionResponse callTool(@RequestBody ToolCallRequest toolCallRequest){
        return toolCallService.call(toolCallRequest);
    }

    @GetMapping
    public List<ToolInfoResponse> listTools(){
        return toolCallService.listTools();
    }

    @GetMapping("/{toolName}")
    public ToolInfoResponse getTool(@PathVariable String toolName) {
        return toolCallService.getTool(toolName);
    }

    @PostMapping("/{toolName}")
    public ToolExecutionResponse callToolByName(@PathVariable String toolName,
                                                @RequestBody(required = false) Map<String, Object> arguments) {
        return toolCallService.call(new ToolCallRequest(toolName, arguments));
    }
}
