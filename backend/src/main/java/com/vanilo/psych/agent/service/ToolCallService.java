package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.*;
import com.vanilo.psych.agent.tool.ToolRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
public class ToolCallService {
    private final ToolRegistry toolRegistry;
    public ToolCallService(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }


    public ToolExecutionResponse call(ToolCallRequest request) {
        if (request == null) {
            throw new RuntimeException("request不能为空");
        }

        String tool = request.getTool();
        Map<String, Object> arguments = request.getArguments();
        if (tool == null || tool.isBlank()) {
            throw new RuntimeException("tool不能为空");
        }
        if (arguments == null) {
            arguments = Map.of();
        }
        validateArguments(toolRegistry.getToolInfo(tool), arguments);
        Object result = toolRegistry.execute(tool, arguments);
        return new ToolExecutionResponse(tool, true, result, LocalDateTime.now());

    }

    public List<ToolInfoResponse> listTools() {
        return toolRegistry.listAllTools();
    }

    public ToolInfoResponse getTool(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            throw new RuntimeException("toolName不能为空");
        }
        return toolRegistry.getToolInfo(toolName);
    }

    private void validateArguments(ToolInfoResponse toolInfo, Map<String, Object> arguments) {
        List<ToolParameterInfo> parameters = toolInfo.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        List<String> errors = new ArrayList<>();
        for (ToolParameterInfo parameter : parameters) {
            Object value = arguments.get(parameter.getName());
            if (parameter.isRequired() && isMissing(value)) {
                errors.add(parameter.getName() + "不能为空");
                continue;
            }
            if (isMissing(value)) {
                continue;
            }
            if (!matchesType(parameter.getType(), value)) {
                errors.add(parameter.getName() + "类型错误，应为" + parameter.getType());
            }
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException(String.join("；", errors));
        }
    }

    private boolean isMissing(Object value) {
        return value == null || (value instanceof String string && string.isBlank());
    }

    private boolean matchesType(String expectedType, Object value) {
        if (expectedType == null || expectedType.isBlank()) {
            return true;
        }
        return switch (expectedType) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Byte
                    || value instanceof Short
                    || value instanceof Integer
                    || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List<?>;
            case "object" -> value instanceof Map<?, ?>;
            default -> Objects.nonNull(value);
        };
    }
}
