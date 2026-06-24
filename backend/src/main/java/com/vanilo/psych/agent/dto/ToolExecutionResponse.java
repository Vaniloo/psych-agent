package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolExecutionResponse {
    private String tool;
    private boolean success;
    private Object result;
    private LocalDateTime executedAt;
}
