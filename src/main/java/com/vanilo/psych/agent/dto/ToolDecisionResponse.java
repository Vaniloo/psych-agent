package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolDecisionResponse {
    private boolean needTool;
    private String tool;
    private Map<String, Object> arguments;
    private String reply;
}
