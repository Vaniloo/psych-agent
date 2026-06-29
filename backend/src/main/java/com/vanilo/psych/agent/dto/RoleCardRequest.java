package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleCardRequest {
    private String name;
    private String description;
    private String tone;
    private String responseStyle;
    private String customInstructions;
    private String forbiddenExpressions;
}
