package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleCardResponse {
    private Long id;
    private String name;
    private String description;
    private String tone;
    private String responseStyle;
    private String customInstructions;
    private String forbiddenExpressions;
    private boolean preset;
    private boolean active;
}
