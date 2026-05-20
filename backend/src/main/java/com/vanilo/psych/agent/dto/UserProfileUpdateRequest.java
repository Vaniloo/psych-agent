package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileUpdateRequest {
    private String profileSummary;
    private String concerns;
    private String preferences;
    private String copingStrategies;
    private String riskSignals;
    private String supportGoals;
}
