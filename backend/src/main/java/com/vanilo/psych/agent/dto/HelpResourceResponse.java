package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HelpResourceResponse {
    private String name;
    private String type;
    private String contact;
    private String description;
    private boolean urgent;
}
