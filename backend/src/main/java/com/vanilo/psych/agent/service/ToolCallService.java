package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.*;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.repository.UserRepository;
import com.vanilo.psych.agent.tool.ToolRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ToolCallService {
    private final ToolRegistry toolRegistry;
    public ToolCallService(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }


    public Object call(ToolCallRequest request) {
        if (request == null) {
            throw new RuntimeException("request不能为空");
        }

        String tool = request.getTool();
        Map<String, Object> arguments = request.getArguments();
        if (tool == null || tool.isBlank()) {
            throw new RuntimeException("tool不能为空");
        }
        if (arguments == null) {
            throw new RuntimeException("arguments不能为空");
        }
        return toolRegistry.execute(tool, arguments);

    }
    public List<ToolInfoResponse> listTools() {
        return toolRegistry.listAllTools();
    }
}