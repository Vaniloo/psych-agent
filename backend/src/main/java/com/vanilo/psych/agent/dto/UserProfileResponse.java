package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private String username;
    private String profileSummary;
    private String concerns;
    private String preferences;
    private String copingStrategies;
    private String riskSignals;
    private String supportGoals;
    private LocalDateTime updatedAt;
}
