package com.vanilo.psych.agent.tool;

import com.vanilo.psych.agent.dto.ToolInfoResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {
    private final Map<String, ToolExecutor> toolMap = new LinkedHashMap<>();

    public ToolRegistry(List<ToolExecutor> tools) {
        for (ToolExecutor tool : tools) {
            if (toolMap.containsKey(tool.getName())) {
                throw new RuntimeException("Tool already exists: " + tool.getName());
            }
            toolMap.put(tool.getName(), tool);
        }
    }

    public Object execute(String toolName, Map<String, Object> arguments) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be null or blank");
        }

        ToolExecutor tool = toolMap.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }

        if (arguments == null) {
            arguments = new HashMap<>();
        }

        return tool.execute(arguments);
    }
    public List<ToolInfoResponse> listAllTools() {
        return toolMap.values().stream()
                .map(ToolExecutor::getToolInfo)
                .toList();
    }
    public boolean contains(String toolName) {
        return toolMap.containsKey(toolName);
    }
}