package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.ToolCallRequest;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.service.ToolCallService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping
    public List<ToolInfoResponse> listTools(){
        return toolCallService.listTools();
    }
}
