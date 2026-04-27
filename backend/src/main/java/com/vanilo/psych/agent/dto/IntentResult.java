package com.vanilo.psych.agent.dto;

import com.vanilo.psych.agent.enums.IntentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntentResult {
    private IntentType intent;
    private Double confidence;
    private String reason;
}
