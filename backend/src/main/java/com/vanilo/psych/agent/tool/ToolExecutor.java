package com.vanilo.psych.agent.tool;

import com.vanilo.psych.agent.dto.ToolInfoResponse;

import java.util.Map;

public interface ToolExecutor {
    String getName();
    ToolInfoResponse getToolInfo();
    Object execute(Map<String, Object> arguments);
}
